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

---
## 正體中文 (Traditional Chinese)

# 批次處理與報告功能 - 設計文件

## 1. 批次處理架構

*   **工作流程協調策略：**
    *   **建議工具：Apache Airflow**
        *   **理由：**
            *   **基於 Python 的 DAG：** 允許彈性且動態的工作流程定義，適用於協調對不同微服務 (AFP 產生、轉換、電子郵件傳送器) 的呼叫。
            *   **可擴展性：** Airflow 的架構包含 worker 和訊息佇列 (Celery、Redis 等)，使其能夠擴展以處理大量並行批次工作。
            *   **豐富的使用者介面：** 為批次工作提供出色的監控、排程和手動觸發功能。
            *   **可擴充性：** 大量現有運算子，並且易於編寫自訂運算子以與我們的微服務互動 (例如，用於 API 呼叫的 `SimpleHttpOperator`、用於自訂邏輯的 `PythonOperator`，或用於訊息佇列互動的自訂運算子)。
            *   **成熟的社群與生態系統：** 獲得良好支援並廣泛採用。
        *   **替代方案 (如果偏好以 Java 為中心的協調)：** 使用 Java 建置的專用**協調服務**，可能利用 Spring 框架。此服務將管理工作定義、排程以及與其他服務的互動。如果需要，可以在此協調器中使用 Spring Batch 執行特定的資料密集型步驟，但 Airflow 通常更適合多服務工作流程管理。
*   **批次工作定義與啟動：**
    *   **定義：**
        *   批次工作將定義為 **Airflow DAG (有向無環圖)**。
        *   每個 DAG 將代表一個特定的批次工作流程 (例如，「產生並傳送每日發票」、「將封存的 AFP 報告轉換為 PDF/A」)。
        *   DAG 將指定任務順序、相依性和參數。
        *   DAG 中的任務通常包括：
            1.  擷取要處理的項目清單 (例如，用於發票的客戶 ID、用於轉換的 AFP 檔案清單)。這可能來自資料庫查詢、檔案或 API 呼叫。
            2.  迭代這些項目並觸發相關的微服務。
    *   **啟動：**
        *   **排程觸發器：** Airflow 的內建排程器將用於基於時間的批次工作 (例如，每天凌晨 2 點、每週日)。Cron 運算式將定義排程。
        *   **手動觸發器：** 可以透過 Airflow UI 或 Airflow 的 CLI/API 手動觸發批次工作，以進行臨時執行或重新執行。
        *   **協調器的 API 觸發器：** 在 Airflow 服務 (或包裝服務) 上公開一個安全的 API 端點，以允許外部系統使用參數觸發特定的 DAG。例如：`POST /api/v1/dags/{dag_id}/dagRuns`。
*   **核心服務之間的資料流程 (非同步處理)：**
    *   **一般原則：** 批次處理將嚴重依賴透過訊息佇列的非同步通訊，以解耦服務並有效處理大量資料。
    *   **範例工作流程：「產生、轉換和透過電子郵件傳送整批發票」**
        1.  **協調器 (Airflow DAG 任務 1 - 資料收集)：**
            *   查詢資料庫 (例如 CRM、帳單系統) 以取得需要產生的發票的客戶 ID 清單和相關資料。
            *   對於每個客戶/發票，它可能會在「產生請求佇列」(例如 RabbitMQ) 中放置一則訊息。此訊息包含客戶資料和範本識別碼。
            ```json
            // 給產生請求佇列的訊息
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "invoice_id": "INV12345",
              "customer_data": { ... },
              "template_id": "invoice_template_v2"
            }
            ```
        2.  **AFP 產生服務 (從產生請求佇列取用)：**
            *   拾取訊息。
            *   呼叫其內部範本管理和資料對應服務。
            *   產生 AFP 檔案。
            *   將 AFP 檔案儲存在共用位置 (例如 S3、Azure Blob、NFS)。
            *   在「轉換請求佇列」中放置一則訊息。
            ```json
            // 給轉換請求佇列的訊息
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "original_invoice_id": "INV12345",
              "afp_file_uri": "s3://afp-bucket/INV12345.afp",
              "target_format": "pdf", // 或 PDF/A
              "output_uri_template": "s3://converted-bucket/INV12345.pdf"
            }
            ```
        3.  **AFP 轉換服務 (從轉換請求佇列取用)：**
            *   拾取訊息。
            *   從 URI 擷取 AFP 檔案。
            *   將其轉換為目標格式 (例如 PDF)。
            *   將轉換後的檔案儲存在指定的輸出 URI。
            *   在「電子郵件傳送請求佇列」中放置一則訊息。
            ```json
            // 給電子郵件傳送請求佇列的訊息
            {
              "batch_job_id": "daily_invoice_run_20250115",
              "original_invoice_id": "INV12345", // 用於追蹤
              "email_request": {
                "from_address": "invoices@example.com",
                "to_addresses": ["customer@email.com"], // 如果需要，根據 invoice_id 擷取
                "subject": "您的發票 INV12345",
                "body_html": "請查收您的附件發票。",
                "attachments": [
                  {
                    "filename": "INV12345.pdf",
                    "content_type": "application/pdf",
                    "download_url": "s3://converted-bucket/INV12345.pdf" // 電子郵件服務擷取此項
                  }
                ]
              }
            }
            ```
        4.  **電子郵件傳送服務 (從電子郵件傳送請求佇列取用)：**
            *   拾取訊息。
            *   建構 MIME 電子郵件。如果附件使用 `download_url`，則如果設定為嵌入，它可能會擷取附件內容，或者僅包含連結。
            *   傳送電子郵件 (根據其設計處理 MX 查閱、重試、DKIM 等)。
            *   更新其內部狀態追蹤資料庫。
        5.  **協調器 (Airflow DAG 任務 2 及之後 - 監控/完成)：**
            *   Airflow 任務可以透過以下方式監控進度：
                *   檢查佇列深度。
                *   查詢微服務的狀態 API (如果可用於批次層級狀態)。
                *   等待完成訊號 (例如，來自鏈中最後一個服務的完成佇列中的最終「摘要」訊息，或透過檢查是否所有預期輸出都存在)。
            *   來自每個服務的日誌對於詳細監控和疑難排解至關重要。

## 2. 高效能與水平可擴展性考量

*   **無狀態服務：**
    *   所有核心微服務 (AFP 產生、轉換、電子郵件傳送器) 和協調器 (Airflow worker) 都應設計為無狀態。
    *   狀態 (例如工作狀態、電子郵件傳送狀態、佇列訊息) 應在外部管理 (資料庫、訊息佇列、必要時使用分散式快取)。
    *   這允許透過在負載平衡器後面簡單地新增每個服務的更多執行個體 (對於 API 觸發的服務) 或擁有更多基於佇列的服務的取用者執行個體來輕鬆實現水平擴展。Airflow 的 CeleryExecutor 或 KubernetesExecutor 即為此目的而設計。
*   **資源利用率優化：**
    *   **AFP 產生服務：**
        *   高效的範本剖析和快取 (如果範本不經常變更)。
        *   如果處理非常大的輸入資料進行對應，則盡可能使用串流式處理，儘管 AFP 本身通常是基於頁面的。
        *   優化資源嵌入 (字型、影像) – 如果可能，使用 Apache FOP 子集化字型。
    *   **AFP 轉換服務：**
        *   商業轉換工具通常針對效能進行了優化。如果使用自訂或開源工具，請確保高效的 I/O 和記憶體管理。
        *   如果在轉換期間進行 OCR (如果適用)，這是 CPU 密集型的。單獨擴展啟用 OCR 的轉換執行個體，或使用可自動擴展的雲端 OCR 服務。
    *   **電子郵件傳送服務：**
        *   用於 SMTP 連線的非同步 I/O (例如 Java NIO、Python asyncio/aiofiles、.NET async/await)，以處理許多並行連線而不會阻塞執行緒。
        *   高效的 MIME 訊息建構。
        *   優化 DNS 查閱 (快取)。
    *   **一般：**
        *   根據每個服務的工作負載設定檔，為其使用適當的執行個體大小 (CPU/記憶體)。
        *   與資料庫和其他下游服務的連線池。
*   **批次資料的資料庫優化技術：**
    *   **批次插入/更新：**
        *   當服務更新其資料庫中的狀態時 (例如，電子郵件傳送器更新多個收件者的狀態)，請使用批次 JDBC/ADO.NET 操作或 ORM 特定的批次處理功能。
        *   Airflow 的資料庫後端也受益於此，用於追蹤任務執行個體。
    *   **連線池：** 對於所有與資料庫互動的服務都至關重要 (例如，Java 的 HikariCP、Python/PostgreSQL 的 `psycopg2` 池、ADO.NET 連線池)。確保調整池大小。
    *   **非同步資料庫寫入 (謹慎)：** 對於非關鍵的類日誌資料，有時可以非同步地對寫入進行排隊或批次處理，以減少主要處理路徑中的延遲。但是，對於工作流程決策至關重要的狀態更新通常應為同步的，或使用可靠的交易式訊息傳遞。
    *   **索引：** 確保在批次查詢中經常使用的 `WHERE` 子句中的欄位 (例如 `status`、`batch_job_id`、`timestamp_ranges`) 上建立適當的資料庫索引。
    *   **資料封存/清除：** 對於歷史批次資料 (工作執行、資料庫中的日誌)，實作封存舊資料的策略，以保持營運資料表的精簡。

## 3. 監控儀表板

*   **建議工具：**
    *   **指標收集：Prometheus**
        *   廣泛採用、基於拉取的指標收集系統。
        *   適用於 Java (Micrometer)、Python、.NET 的用戶端函式庫。
        *   Airflow 具有 Prometheus 匯出器。
    *   **視覺化：Grafana**
        *   強大的視覺化工具，可與 Prometheus 無縫整合。
        *   允許建立豐富的互動式儀表板。
    *   **日誌聚合：ELK Stack (Elasticsearch、Logstash、Kibana) 或 Grafana Loki**
        *   對於所有微服務和 Airflow 元件的集中式日誌記錄和搜尋至關重要。
*   **要收集的關鍵指標：**
    *   **協調器 (Airflow)：**
        *   `airflow_dag_run_states` (依狀態計算的 DAG 執行次數：執行中、成功、失敗)
        *   `airflow_task_instance_states` (依狀態計算的任務執行個體次數)
        *   `airflow_executor_queue_size` (如果使用 CeleryExecutor)
        *   `airflow_scheduler_heartbeat`
        *   `dag_processing_duration_seconds` (DAG 執行持續時間)
    *   **AFP 產生服務：**
        *   `afp_generation_requests_total` (計數器)
        *   `afp_generation_success_total` (計數器)
        *   `afp_generation_failure_total` (計數器，帶有錯誤類型標籤)
        *   `afp_generation_duration_seconds` (長條圖/摘要)
        *   `queue_depth_generation_request` (量規，如果從佇列取用)
    *   **AFP 轉換服務：**
        *   `afp_conversion_requests_total` (計數器，目標格式的標籤)
        *   `afp_conversion_success_total` (計數器)
        *   `afp_conversion_failure_total` (計數器，帶有錯誤類型標籤)
        *   `afp_conversion_duration_seconds` (長條圖/摘要)
        *   `queue_depth_conversion_request` (量規)
    *   **電子郵件傳送服務：**
        *   `email_sending_requests_total` (計數器)
        *   `email_sent_success_total` (計數器)
        *   `email_sent_failure_total` (計數器，錯誤類型標籤，例如 'bounce_permanent', 'retry_timeout')
        *   `email_sending_duration_seconds` (長條圖/摘要)
        *   `queue_depth_email_sending_request` (量規)
        *   `dkim_signing_success_total` / `dkim_signing_failure_total`
        *   `smtp_connection_status` (例如，與 MX 伺服器成功與失敗連線的次數)
    *   **所有服務的通用指標：**
        *   每個執行個體的 CPU/記憶體/網路利用率。
        *   GC 統計資料 (適用於 Java/.NET)。
        *   API 端點延遲和錯誤率 (例如 HTTP 5xx)。
        *   資料庫連線池指標 (作用中連線、閒置連線、等待時間)。
*   **儀表板範例 (Grafana)：**
    *   **整體系統健康狀況：**
        *   來自每個服務的關鍵狀態指標 (運作中/關閉、錯誤率)。
        *   作用中批次工作概觀 (來自 Airflow)。
        *   全域錯誤率趨勢。
    *   **AFP 產生儀表板：**
        *   產生請求率、成功/失敗率。
        *   產生持續時間百分位數。
        *   產生佇列待辦事項。
        *   主要錯誤。
    *   **AFP 轉換儀表板：**
        *   轉換請求率、成功/失敗率 (可依目標格式篩選)。
        *   轉換持續時間百分位數。
        *   轉換佇列待辦事項。
    *   **電子郵件傳送儀表板：**
        *   傳送率、成功/失敗/退信率。
        *   傳送持續時間百分位數。
        *   電子郵件傳送佇列待辦事項。
        *   DKIM 簽署成功率。
        *   依 SMTP 錯誤代碼或退信類型細分的失敗。
    *   **批次工作狀態儀表板 (Airflow 資料)：**
        *   作用中 DAG 執行及其狀態清單。
        *   一段時間內關鍵 DAG 的成功/失敗趨勢。
        *   所選 DAG 執行中的任務執行個體狀態和持續時間。
        *   Airflow worker 的資源使用情況。

## 4. 報告功能 (每日/每週)

*   **報告元件/服務：**
    *   **選項 1 (Airflow DAG)：** 可以排程 Airflow DAG 以產生報告。此 DAG 將：
        *   從 Prometheus (透過其 API) 或儲存狀態的應用程式資料庫查詢指標。
        *   聚合資料。
        *   格式化報告 (例如 HTML、PDF、CSV)。
        *   分發報告。
    *   **選項 2 (專用報告微服務)：** 一個小型服務，訂閱相關事件或定期查詢資料來源以編譯報告。如果 Airflow 可以處理，這最初可能過於複雜。
    *   **建議：** 為簡單起見，從 Airflow DAG 開始。
*   **報告內容與格式：**
    *   **目標對象：** 營運團隊、業務利害關係人。
    *   **內容 (範例)：**
        *   **摘要區段：**
            *   已處理文件總數 (已產生、已轉換、已透過電子郵件傳送)。
            *   關鍵批次工作的整體成功/失敗率。
            *   關鍵傳送能力指標 (例如，電子郵件的整體退信率)。
        *   **AFP 產生：**
            *   產生的 AFP 數量。
            *   錯誤率、主要錯誤。
        *   **AFP 轉換：**
            *   轉換的檔案數量 (依類型)。
            *   錯誤率。
        *   **電子郵件傳送：**
            *   嘗試傳送、已傳送、失敗 (永久)、退回的電子郵件總數。
            *   如果可能，細分失敗/退信。
            *   與前一期間 (例如，逐週) 的比較趨勢。
        *   **批次工作效能：**
            *   主要排程批次工作的狀態 (成功/失敗次數)。
            *   平均執行時間。
    *   **格式：**
        *   **HTML 電子郵件：** 適用於快速摘要和可讀性，可以包含指向 Grafana 儀表板的連結以取得詳細資訊。
        *   **PDF：** 用於更正式的報告或封存。
        *   **CSV：** 用於可能匯入其他系統的原始資料。
*   **報告分發機制：**
    *   **電子郵件：** 報告 DAG/服務可以使用電子郵件傳送服務本身 (或者為簡單起見，如果偏好用於內部系統報告，則使用直接 SMTP 函式庫) 將報告傳送到設定的收件者清單。
    *   **儲存：** 報告可以儲存在指定位置 (例如 S3、SharePoint) 以供歷史存取。
    *   **儀表板連結：** 報告應大量參考並連結到即時 Grafana 儀表板，以便進行更詳細的互動式探索。

## 5. 排程與即時處理共存

*   **建議的排程工具/方法：**
    *   **Apache Airflow：** 作為協調選擇，Airflow 具有使用 cron 運算式的強大排程功能。這是排程批次工作的主要機制。
    *   **作業系統層級 Cron (後備/簡單案例)：** 對於不需要複雜工作流程管理的非常簡單的獨立任務 (例如，透過 API 觸發 Airflow DAG 的指令碼)，可以使用 cron。但是，在 Airflow 中管理複雜相依性和監控效果更好。
*   **支援排程批次和臨時即時請求：**
    *   **不同的進入點：**
        *   **即時：** 個別微服務 (AFP 產生、轉換、電子郵件傳送器) 具有自己的 API 端點，用於單一的隨選請求。這些應設計為低延遲。
        *   **批次：** 批次工作透過 Airflow (排程或透過其 UI/API 手動觸發) 啟動。然後 Airflow 與服務互動，可能是透過其 API 或將訊息放置在其取用的佇列中。
    *   **資源隔離/優先順序設定：**
        *   **專用佇列：** 如果使用訊息佇列，請考慮為即時請求與批次起始的請求使用不同的佇列 (如果需要嚴格的優先順序設定)。即時佇列可以有更多取用者或更高優先順序的取用者。
        *   **服務執行個體擴展：** 可以根據組合負載擴展服務。如果即時負載具有突發性，自動擴展機制 (例如，基於 CPU/記憶體或佇列長度等自訂指標的 Kubernetes HPA) 會有所幫助。
        *   **API 速率限制：** 保護即時 API 免受設定錯誤的批次工作或其他用戶端的影響。
    *   **資料庫負載：** 確保資料庫效能足以處理來自即時交易和批次處理的並行存取。批次操作應設計為盡可能高效 (批次 DML、最小化長交易)。
*   **彈性分派與優先順序設定：**
    *   **Airflow 集區與優先順序：** Airflow 允許定義 worker 插槽的「集區」並將優先順序指派給 DAG 或任務。這可以確保高優先順序的批次工作在低優先順序的工作之前取得資源。
    *   **訊息佇列優先順序：** RabbitMQ 和其他訊息代理程式支援訊息優先順序。協調器可以在傳送到服務佇列的訊息上設定優先順序。取用這些訊息的服務需要設定為遵循這些優先順序 (例如，為不同優先順序層級設定不同的取用者集區)。
    *   **API 閘道層級 (用於即時)：** API 閘道可以為不同的 API 用戶端或請求類型實作請求節流或優先順序設定規則，儘管這更適用於即時路徑管理。
    *   **最小化干擾設計：** 避免干擾的主要方法是透過非同步處理 (佇列) 和為每個服務適當分配/擴展資源。如果批次工作為電子郵件傳送服務產生一百萬則訊息，則電子郵件傳送服務應能夠以自己的步調取用這些訊息，而不會影響其處理緊急電子郵件的新單一 API 請求的能力 (假設其 API 和佇列取用者已適當設定和擴展)。

本文件為實作批次處理和報告功能提供了全面的策略。關鍵在於健全的協調層 (Airflow)、非同步通訊、無狀態服務和徹底的監控。
