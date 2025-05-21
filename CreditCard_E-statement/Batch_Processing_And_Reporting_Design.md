# Batch Processing and Reporting Features - Design Document

## 1. Batch Processing Architecture

*   **Workflow Orchestration Strategy:**
    *   **Recommended Tool: Apache Airflow**
        *   **Reasoning:**
            *   **Python-based DAGs:** Allows for flexible and dynamic workflow definition, which is suitable for orchestrating calls to diverse microservices (AFP Generation, Conversion, Email Sender).
            *   **Scalability:** Airflow's architecture with workers and a message queue (Celery, Redis, etc.) allows it to scale for handling numerous concurrent batch jobs.
            *   **Rich UI:** Provides excellent monitoring, scheduling, and manual triggering capabilities for batch jobs.
            *   **Extensibility:** Large number of existing operators and easy to write custom operators to interact with our microservices (e.g., `SimpleHttpOperator` for API calls, `PythonOperator` for custom logic, or custom operators for message queue interactions).
            *   **Mature Community & Ecosystem:** Well-supported and widely adopted.
        *   **Alternative (if Java-centric orchestration is preferred):** A dedicated **Orchestration Service** built using Java, potentially leveraging Spring frameworks. This service would manage job definitions, scheduling, and interactions with other services. Spring Batch could be used *within* this orchestrator for specific data-intensive steps if needed, but Airflow is generally better for multi-service workflow management.
*   **Batch Job Definition and Initiation:**
    *   **Definition:**
        *   Batch jobs will be defined as **Airflow DAGs (Directed Acyclic Graphs)**.
        *   Each DAG will represent a specific batch workflow (e.g., "Generate and Send Daily Invoices," "Convert Archived AFP Reports to PDF/A").
        *   DAGs will specify the sequence of tasks, dependencies, and parameters.
        *   Tasks within a DAG will typically involve:
            1.  Fetching a list of items to process (e.g., customer IDs for invoices, list of AFP files for conversion). This might come from a database query, a file, or an API call.
            2.  Iterating through these items and triggering the relevant microservices.
    *   **Initiation:**
        *   **Scheduled Triggers:** Airflow's built-in scheduler will be used for time-based batch jobs (e.g., daily at 2 AM, weekly on Sunday). Cron expressions will define schedules.
        *   **Manual Triggers:** Batch jobs can be triggered manually via the Airflow UI or Airflow's CLI/API for ad-hoc runs or reruns.
        *   **API Trigger for Orchestrator:** Expose a secure API endpoint on the Airflow service (or a wrapper service) to allow external systems to trigger specific DAGs with parameters. For example: `POST /api/v1/dags/{dag_id}/dagRuns`.
*   **Data Flow Between Core Services (Asynchronous Processing):**
    *   **General Principle:** Batch processing will heavily rely on asynchronous communication via message queues to decouple services and handle large volumes efficiently.
    *   **Example Workflow: "Generate, Convert, and Email Batch of Invoices"**
        1.  **Orchestrator (Airflow DAG Task 1 - Data Collection):**
            *   Queries a database (e.g., CRM, Billing System) to get a list of customer IDs and associated data required for invoices that need to be generated.
            *   For each customer/invoice, it might place a message on a "Generation Request Queue" (e.g., RabbitMQ). This message contains customer data and a template identifier.
            ```json
            // Message to Generation Request Queue
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "invoice_id": "INV12345",
              "customer_data": { ... },
              "template_id": "invoice_template_v2"
            }
            ```
        2.  **AFP Generation Service (Consumes from Generation Request Queue):**
            *   Picks up the message.
            *   Calls its internal template management and data mapping services.
            *   Generates the AFP file.
            *   Stores the AFP file in a shared location (e.g., S3, Azure Blob, NFS).
            *   Places a message on a "Conversion Request Queue."
            ```json
            // Message to Conversion Request Queue
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "original_invoice_id": "INV12345",
              "afp_file_uri": "s3://afp-bucket/INV12345.afp",
              "target_format": "pdf", // Or PDF/A
              "output_uri_template": "s3://converted-bucket/INV12345.pdf"
            }
            ```
        3.  **AFP Conversion Service (Consumes from Conversion Request Queue):**
            *   Picks up the message.
            *   Retrieves the AFP file from the URI.
            *   Converts it to the target format (e.g., PDF).
            *   Stores the converted file at the specified output URI.
            *   Places a message on an "Email Sending Request Queue."
            ```json
            // Message to Email Sending Request Queue
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "original_invoice_id": "INV12345", // For tracking
              "email_request": {
                "from_address": "invoices@example.com",
                "to_addresses": ["customer@email.com"], // Fetched based on invoice_id if needed
                "subject": "Your Invoice INV12345",
                "body_html": "Please find your invoice attached.",
                "attachments": [
                  {
                    "filename": "INV12345.pdf",
                    "content_type": "application/pdf",
                    "download_url": "s3://converted-bucket/INV12345.pdf" // Email service fetches this
                  }
                ]
              }
            }
            ```
        4.  **Email Sender Service (Consumes from Email Sending Request Queue):**
            *   Picks up the message.
            *   Constructs the MIME email. If `download_url` is used for attachments, it might fetch the attachment content if configured to embed, or just include the link.
            *   Sends the email (handling MX lookups, retries, DKIM, etc., as per its design).
            *   Updates its internal status tracking database.
        5.  **Orchestrator (Airflow DAG Task 2 onwards - Monitoring/Completion):**
            *   Airflow tasks can monitor the progress by:
                *   Checking the depth of queues.
                *   Querying status APIs of the microservices (if available for batch-level status).
                *   Waiting for completion signals (e.g., a final "summary" message on a completion queue from the last service in the chain, or by checking if all expected outputs exist).
            *   Logs from each service will be crucial for detailed monitoring and troubleshooting.

## 2. High-Performance and Horizontal Scalability Considerations

*   **Stateless Services:**
    *   All core microservices (AFP Generation, Conversion, Email Sender) and the Orchestrator (Airflow workers) should be designed to be stateless.
    *   State (e.g., job status, email sending status, queue messages) should be managed externally (databases, message queues, distributed cache if necessary).
    *   This allows for easy horizontal scaling by simply adding more instances of each service behind a load balancer (for API-triggered services) or by having more consumer instances for queue-based services. Airflow's CeleryExecutor or KubernetesExecutor are designed for this.
*   **Optimizations for Resource Utilization:**
    *   **AFP Generation Service:**
        *   Efficient template parsing and caching (if templates don't change often).
        *   Stream-based processing where possible if dealing with very large input data for mapping, though AFP itself is often page-based.
        *   Optimize resource embedding (fonts, images) – subset fonts if possible with Apache FOP.
    *   **AFP Conversion Service:**
        *   Commercial conversion tools are often optimized for performance. If using custom or open-source, ensure efficient I/O and memory management.
        *   For OCR during conversion (if applicable), this is CPU-intensive. Scale OCR-enabled conversion instances separately or use cloud OCR services that scale automatically.
    *   **Email Sender Service:**
        *   Asynchronous I/O for SMTP connections (e.g., Java NIO, Python asyncio/aiofiles, .NET async/await) to handle many concurrent connections without blocking threads.
        *   Efficient MIME message construction.
        *   Optimize DNS lookups (caching).
    *   **General:**
        *   Use appropriate instance sizes (CPU/memory) for each service based on its workload profile.
        *   Connection pooling to databases and other downstream services.
*   **Database Optimization Techniques for Batch Data:**
    *   **Batch Inserts/Updates:**
        *   When services update status in their databases (e.g., Email Sender updating status for multiple recipients), use batch JDBC/ADO.NET operations or ORM-specific batching features.
        *   Airflow's database backend also benefits from this for tracking task instances.
    *   **Connection Pooling:** Essential for all services interacting with databases (e.g., HikariCP for Java, `psycopg2`'s pooling for Python/PostgreSQL, ADO.NET connection pooling). Ensure pool sizes are tuned.
    *   **Asynchronous Database Writes (Carefully):** For non-critical log-like data, writes can sometimes be queued or batched asynchronously to reduce latency in the main processing path. However, status updates critical for workflow decisions should generally be synchronous or use reliable transactional messaging.
    *   **Indexing:** Ensure appropriate database indexes on columns frequently used in `WHERE` clauses for batch queries (e.g., `status`, `batch_job_id`, `timestamp_ranges`).
    *   **Data Archiving/Purging:** For historical batch data (job runs, logs in DB), implement strategies for archiving old data to keep operational tables lean.

## 3. Monitoring Dashboards

*   **Recommended Tools:**
    *   **Metrics Collection: Prometheus**
        *   Widely adopted, pull-based metrics collection system.
        *   Client libraries available for Java (Micrometer), Python, .NET.
        *   Airflow has a Prometheus exporter.
    *   **Visualization: Grafana**
        *   Powerful visualization tool that integrates seamlessly with Prometheus.
        *   Allows for creating rich, interactive dashboards.
    *   **Log Aggregation: ELK Stack (Elasticsearch, Logstash, Kibana) or Grafana Loki**
        *   Essential for centralized logging and searching across all microservices and Airflow components.
*   **Key Metrics to Collect:**
    *   **Orchestrator (Airflow):**
        *   `airflow_dag_run_states` (count of DAG runs by state: running, success, failed)
        *   `airflow_task_instance_states` (count of task instances by state)
        *   `airflow_executor_queue_size` (if using CeleryExecutor)
        *   `airflow_scheduler_heartbeat`
        *   `dag_processing_duration_seconds` (duration of DAG runs)
    *   **AFP Generation Service:**
        *   `afp_generation_requests_total` (counter)
        *   `afp_generation_success_total` (counter)
        *   `afp_generation_failure_total` (counter, with error type label)
        *   `afp_generation_duration_seconds` (histogram/summary)
        *   `queue_depth_generation_request` (gauge, if consuming from a queue)
    *   **AFP Conversion Service:**
        *   `afp_conversion_requests_total` (counter, labels for target_format)
        *   `afp_conversion_success_total` (counter)
        *   `afp_conversion_failure_total` (counter, with error type label)
        *   `afp_conversion_duration_seconds` (histogram/summary)
        *   `queue_depth_conversion_request` (gauge)
    *   **Email Sender Service:**
        *   `email_sending_requests_total` (counter)
        *   `email_sent_success_total` (counter)
        *   `email_sent_failure_total` (counter, labels for error type e.g., 'bounce_permanent', 'retry_timeout')
        *   `email_sending_duration_seconds` (histogram/summary)
        *   `queue_depth_email_sending_request` (gauge)
        *   `dkim_signing_success_total` / `dkim_signing_failure_total`
        *   `smtp_connection_status` (e.g., count of successful vs. failed connections to MX servers)
    *   **Common Metrics for all Services:**
        *   CPU/Memory/Network utilization per instance.
        *   GC statistics (for Java/ .NET).
        *   API endpoint latency and error rates (e.g., HTTP 5xx).
        *   Database connection pool metrics (active connections, idle, wait times).
*   **Examples of Dashboards (Grafana):**
    *   **Overall System Health:**
        *   Key status indicators from each service (up/down, error rates).
        *   Active batch jobs overview (from Airflow).
        *   Global error rate trends.
    *   **AFP Generation Dashboard:**
        *   Generation request rate, success/failure rates.
        *   Generation duration percentiles.
        *   Queue backlog for generation.
        *   Top errors.
    *   **AFP Conversion Dashboard:**
        *   Conversion request rate, success/failure rates (filterable by target format).
        *   Conversion duration percentiles.
        *   Queue backlog for conversion.
    *   **Email Sending Dashboard:**
        *   Sending rate, success/failure/bounce rates.
        *   Sending duration percentiles.
        *   Queue backlog for email sending.
        *   DKIM signing success rate.
        *   Breakdown of failures by SMTP error codes or bounce types.
    *   **Batch Job Status Dashboard (Airflow data):**
        *   List of active DAG runs and their status.
        *   Success/failure trends for key DAGs over time.
        *   Task instance status and durations within a selected DAG run.
        *   Resource usage by Airflow workers.

## 4. Reporting Features (Daily/Weekly)

*   **Reporting Component/Service:**
    *   **Option 1 (Airflow DAG):** An Airflow DAG can be scheduled to generate reports. This DAG would:
        *   Query metrics from Prometheus (via its API) or the application databases where status is stored.
        *   Aggregate the data.
        *   Format the report (e.g., HTML, PDF, CSV).
        *   Distribute the report.
    *   **Option 2 (Dedicated Reporting Microservice):** A small service that subscribes to relevant events or periodically queries data sources to compile reports. This might be overkill initially if Airflow can handle it.
    *   **Recommendation:** Start with an Airflow DAG for simplicity.
*   **Content and Format of Reports:**
    *   **Target Audience:** Operations team, business stakeholders.
    *   **Content (Examples):**
        *   **Summary Section:**
            *   Total documents processed (generated, converted, emailed).
            *   Overall success/failure rates for key batch jobs.
            *   Key deliverability metrics (e.g., overall bounce rate for emails).
        *   **AFP Generation:**
            *   Number of AFPs generated.
            *   Error rates, top errors.
        *   **AFP Conversion:**
            *   Number of files converted (by type).
            *   Error rates.
        *   **Email Sending:**
            *   Total emails attempted, sent, failed (permanent), bounced.
            *   Breakdown of failures/bounces if possible.
            *   Trends compared to previous period (e.g., week-over-week).
        *   **Batch Job Performance:**
            *   Status of major scheduled batch jobs (success/failure counts).
            *   Average run times.
    *   **Format:**
        *   **HTML Email:** Good for quick summaries and readability, can include links to Grafana dashboards for details.
        *   **PDF:** For more formal reports or archiving.
        *   **CSV:** For raw data that might be imported into other systems.
*   **Report Distribution Mechanisms:**
    *   **Email:** The reporting DAG/service can use the Email Sender Service itself (or a direct SMTP library for simplicity if preferred for internal system reports) to send reports to a configured list of recipients.
    *   **Storage:** Reports can be stored in a designated location (e.g., S3, SharePoint) for historical access.
    *   **Dashboard Link:** Reports should heavily reference and link to the live Grafana dashboards for more detailed, interactive exploration.

## 5. Scheduling and Real-Time Processing Coexistence

*   **Recommended Tools/Approaches for Scheduling:**
    *   **Apache Airflow:** As chosen for orchestration, Airflow has robust scheduling capabilities using cron expressions. This is the primary mechanism for scheduled batch jobs.
    *   **OS-level Cron (Fallback/Simple Cases):** For very simple, standalone tasks that don't require complex workflow management (e.g., a script to trigger an Airflow DAG via API), cron can be used. However, managing complex dependencies and monitoring is better done in Airflow.
*   **Supporting Both Scheduled Batch and Ad-Hoc Real-Time Requests:**
    *   **Separate Entry Points:**
        *   **Real-time:** Individual microservices (AFP Gen, Conversion, Email Sender) have their own API endpoints for single, on-demand requests. These should be designed for low latency.
        *   **Batch:** Batch jobs are initiated via Airflow (scheduled or manually triggered via its UI/API). Airflow then interacts with the services, likely via their APIs or by placing messages on queues they consume.
    *   **Resource Isolation/Prioritization:**
        *   **Dedicated Queues:** If using message queues, consider separate queues for real-time requests vs. batch-initiated requests if strict prioritization is needed. Real-time queues could have more consumers or higher priority consumers.
        *   **Service Instance Scaling:** Services can be scaled based on the combined load. If real-time load is spiky, auto-scaling mechanisms (e.g., Kubernetes HPA based on CPU/memory or custom metrics like queue length) can help.
        *   **Rate Limiting on APIs:** Protect real-time APIs from being overwhelmed by misconfigured batch jobs or other clients.
    *   **Database Load:** Ensure database performance is sufficient to handle concurrent access from both real-time transactions and batch processing. Batch operations should be designed to be as efficient as possible (batch DML, minimizing long transactions).
*   **Flexible Dispatching and Prioritization:**
    *   **Airflow Pools & Priorities:** Airflow allows defining "pools" of worker slots and assigning priorities to DAGs or tasks. This can ensure that high-priority batch jobs get resources before lower-priority ones.
    *   **Message Queue Priorities:** RabbitMQ and other message brokers support message priorities. The orchestrator can set priorities on messages sent to service queues. Services consuming these messages would need to be configured to respect these priorities (e.g., by having separate consumer pools for different priority levels).
    *   **API Gateway Level (for real-time):** An API Gateway could implement request throttling or prioritization rules for different API clients or request types, though this is more for real-time path management.
    *   **Design for Interference Minimization:** The primary way to avoid interference is through asynchronous processing (queues) and proper resource allocation/scaling for each service. If a batch job generates a million messages for the Email Sender Service, the Email Sender Service should be able to consume these at its own pace without impacting its ability to handle a new, single API request for an urgent email (assuming its API and queue consumers are appropriately configured and scaled).

This document provides a comprehensive strategy for implementing batch processing and reporting. The key is a robust orchestration layer (Airflow), asynchronous communication, stateless services, and thorough monitoring.The design document for "Batch Processing and Reporting Features" has been created. It outlines Apache Airflow as the primary orchestration tool, details batch job definition and data flow using message queues, and covers strategies for scalability, monitoring with Prometheus/Grafana, reporting, and coexistence of batch and real-time processing.

I will now submit the report for this subtask.
