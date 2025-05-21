# Logging and Auditing Strategy

This document defines the unified strategy for logging application events, errors, and security-relevant audit trails across all modules of the CreditCard E-statement system. Effective logging and auditing are crucial for monitoring system health, troubleshooting issues, security analysis, and meeting compliance requirements.

## 1. General Logging Principles

*   **Structured Logging:**
    *   **Format:** All application logs MUST be in a structured format, preferably JSON. Each log entry should be a single JSON object.
    *   **Benefits:** Enables easier parsing, filtering, searching, and analysis in centralized logging systems. Allows for consistent field names and data types.
*   **Log Levels:**
    *   Standard log levels (DEBUG, INFO, WARN, ERROR, CRITICAL/FATAL) MUST be used.
    *   **DEBUG:** Fine-grained information useful for developers during troubleshooting. Should be disabled in production by default.
    *   **INFO:** High-level information about normal application behavior and significant lifecycle events (e.g., service startup, request processing, batch job completion).
    *   **WARN:** Indicates potential issues or unexpected situations that are not yet critical errors but might lead to problems if not addressed (e.g., deprecated API usage, resource nearing capacity).
    *   **ERROR:** Errors that prevent a specific operation from completing but do not necessarily shut down the service. The application can often recover or continue with other operations.
    *   **CRITICAL/FATAL:** Severe errors that cause the application or a critical component to terminate or become unstable.
    *   Log levels MUST be configurable per environment and per service. Production environments should default to INFO or WARN.
*   **Correlation IDs:**
    *   A unique Correlation ID (e.g., `trace_id`, `request_id`) MUST be generated at the entry point of a request (e.g., API Gateway, initial job submission) or retrieved from an incoming request header if present.
    *   This ID MUST be propagated across all microservice calls and included in every log entry related to that specific request or transaction. This allows for tracing the entire lifecycle of an operation across distributed services.
*   **Timestamps:**
    *   All log entries MUST include an accurate, high-precision timestamp indicating when the event occurred.
    *   Timestamps SHOULD be in UTC format to avoid timezone ambiguities. Example format: ISO 8601 (`YYYY-MM-DDTHH:mm:ss.sssZ`).
*   **Contextual Information:**
    *   Log entries MUST include relevant contextual information to aid in understanding and debugging. Common context fields include:
        *   `service_name`: Name of the microservice generating the log.
        *   `service_version`: Version of the service.
        *   `hostname` / `instance_id`: Identifier of the specific instance running the service.
        *   `thread_id`: (If applicable)
        *   `user_id`: Identifier of the user who initiated the action (if applicable and available).
        *   `customer_id`: Relevant customer identifier.
        *   `job_id` / `batch_id`: Identifiers for batch processes or specific jobs.
        *   `class_name` / `method_name`: Origin of the log within the code.
*   **Sensitive Data Handling in Logs:**
    *   Strictly AVOID logging sensitive Personally Identifiable Information (PII) (e.g., full credit card numbers, bank account details, national IDs, complete AFP/PDF content, passwords, API keys, access tokens) in plain text.
    *   If necessary for debugging, sensitive data MUST be masked (e.g., showing only last 4 digits of an account number) or tokenized.
    *   Establish clear guidelines on what constitutes sensitive data and how it should be handled in logs.
    *   Regularly review log content to ensure compliance with this principle.

## 2. Types of Logs

*   **Event Logs (Application Logs):**
    *   Record significant, non-error events in the application's lifecycle and business logic execution.
    *   **Examples:**
        *   Service startup and shutdown sequences.
        *   API request received and response sent (summary, not full body unless specifically needed and sanitized).
        *   Message consumed from a queue or published to a queue.
        *   Successful AFP generation, PDF conversion, email assembly.
        *   Email dispatched to SMTP server, SMS sent to gateway.
        *   Batch job started, progress checkpoints, completed successfully.
        *   Key business process milestones.
*   **Error Logs:**
    *   Record all errors and exceptions that occur within applications.
    *   MUST include the full stack trace for exceptions.
    *   MUST include relevant contextual information (correlation ID, input parameters if safe, current state) to help diagnose the error.
    *   Differentiate between handled exceptions (where the application recovers) and unhandled exceptions (which might lead to service termination).
*   **Access Logs:**
    *   Typically generated by web servers, API Gateways, or load balancers.
    *   Log all incoming HTTP requests, including: source IP address, timestamp, HTTP method, requested URL/path, HTTP status code, response size, response time, user agent, referrer.
    *   These are crucial for traffic analysis, security monitoring, and identifying malicious activity.
*   **Debug Logs:**
    *   Provide detailed diagnostic information for developers during troubleshooting.
    *   May include variable values, detailed step-by-step execution paths.
    *   Should be disabled by default in production environments due to volume and potential performance impact. Can be enabled dynamically for specific components or requests if needed for live debugging.

## 3. Audit Trails (Audit Logs)

*   **Purpose:**
    *   Provide a chronological record of security-relevant events and actions performed by users or systems.
    *   Essential for compliance with regulations (e.g., GDPR, SOX, HIPAA if applicable), security analysis, forensic investigations, and establishing non-repudiation.
    *   Audit logs MUST be protected from unauthorized modification or deletion (tamper-evident or tamper-resistant storage).
*   **Key Events to Audit:**
    *   **Authentication & Authorization Events:**
        *   User login attempts (success, failure, source IP).
        *   User logouts.
        *   API client authentication attempts (success, failure).
        *   Password changes, MFA status changes/enrollment.
        *   Session creation/invalidation.
    *   **Access Control Changes:**
        *   Creation, modification, deletion of user accounts.
        *   Assignment or revocation of roles and permissions.
        *   Changes to access control policies.
    *   **Resource Lifecycle & Management:**
        *   **Templates:** Creation, modification, deletion, activation/deactivation of AFP, email, SMS, push templates.
        *   **Communication Jobs:** Submission, cancellation, manual retry/resend actions (including who initiated the action, what was changed if anything, and when).
        *   **Customer Preferences:** Changes to customer communication preferences, contact details, and consent status (including source of change).
    *   **Sensitive Data Access (if applicable):**
        *   Any action where a user or system explicitly views or exports sensitive data (e.g., an admin viewing detailed PII or full statement content, if such functionality exists and is permissible).
    *   **System Configuration Changes:**
        *   Modifications to critical system configurations (e.g., SMTP server settings, API Gateway policies, security settings, log levels, retention policies).
    *   **Security System Events:**
        *   Detected security alerts from WAF, IDS/IPS.
        *   Execution of security-related administrative tasks.
        *   Changes to encryption key status (if managed by application).
*   **Audit Log Content:**
    *   **Timestamp (UTC):** Precise time of the event.
    *   **Actor/Subject:** User ID, service account ID, or system process performing the action.
    *   **Action:** Description of the action performed (e.g., "USER_LOGIN_SUCCESS", "TEMPLATE_CREATED", "MANUAL_EMAIL_RESEND").
    *   **Target/Object:** The resource or entity being acted upon (e.g., template ID, customer ID, configuration setting name).
    *   **Outcome:** Success or failure of the action.
    *   **Source IP Address:** IP address from which the action originated.
    *   **Additional Details:** Any relevant parameters or context (e.g., changed values, reason for action if provided).
*   **Protection and Retention:** Audit logs should be stored securely, with strict access controls, and retained according to legal and business requirements (see `Data_Retention_And_Archival_Policy.md`). Consider write-once-read-many (WORM) storage characteristics if possible.

## 4. Logging Infrastructure & Tools

This section consolidates and refers to choices made in `Technology_Stack_and_Infrastructure.md`.

*   **Log Collection:**
    *   Use standardized logging libraries within each microservice (e.g., SLF4j with Logback/Log4j2 for Java, Python's `logging` module, Serilog/NLog for .NET).
    *   Container logs (stdout/stderr) will be collected by the container orchestration platform (Kubernetes).
    *   Dedicated log shipping agents (e.g., Filebeat, Fluentd, cloud provider agents like CloudWatch Agent, Azure Monitor Agent) can be used for collecting logs from various sources.
*   **Log Aggregation, Storage, and Processing:**
    *   **ELK Stack (Elasticsearch, Logstash, Kibana):**
        *   **Elasticsearch:** For storing, indexing, and searching large volumes of log data.
        *   **Logstash (or Fluentd):** For collecting, parsing, transforming, and enriching logs before sending them to Elasticsearch.
        *   **Kibana:** For visualizing, exploring, and creating dashboards from log data.
    *   **Grafana Loki:** A lightweight, Prometheus-inspired logging backend. Good for environments already using Grafana for metrics.
    *   Choice depends on operational familiarity, scale, and feature requirements.
*   **Log Format Standardization:** All services must emit logs in the agreed-upon structured JSON format to ensure consistent parsing and indexing in the centralized system.
*   **Log Retention:** Policies for log retention (hot storage vs. cold/archival storage) will be defined in `Data_Retention_And_Archival_Policy.md`.
*   **Monitoring and Alerting on Logs:**
    *   Utilize features within the chosen logging stack (e.g., ElastAlert for ELK, Grafana alerting for Loki) to create alerts based on specific log patterns, error counts, or security event IDs.
    *   Examples: Alert on high rate of ERROR/CRITICAL messages, multiple failed login attempts, specific security event signatures.

## 5. Module-Specific Logging Considerations

While adhering to general principles, specific modules may have unique logging needs:

*   **Core AFP Generation Engine:**
    *   Log template ID used, source of input data (e.g., request ID, batch ID, not the full data itself unless specifically required and masked).
    *   Log success/failure of generation, time taken, and any errors from Apache FOP.
    *   Log usage of AFP resources (fonts, overlays, images).
*   **AFP Conversion Module:**
    *   Log input AFP file identifier, target conversion format.
    *   Log details of the external conversion tool being invoked (command, parameters if CLI).
    *   Log success/failure, time taken for conversion, and any error messages or exit codes from the external tool.
*   **Email/SMS/AppPush Sender Services:**
    *   Log unique message ID, recipient identifier (masked if PII and not essential for the log entry's purpose).
    *   Log interactions with gateways/SMTP servers (e.g., request sent, response received, status updates/callbacks).
    *   Log detailed delivery status (queued, sent, failed, bounced, delivered if available).
    *   Log errors related to message assembly, DKIM signing, or gateway communication.
*   **Batch Processing (Airflow/Orchestrator):**
    *   Airflow provides extensive logging for DAG and task execution. These logs should be captured by the centralized logging system.
    *   Log key parameters for each batch run, number of items to be processed, progress, and final outcome (success, failure, partial success).
*   **RESTful API Module (API Gateway & Services):**
    *   API Gateway will provide detailed access logs.
    *   Backend services should log the start and end of API request processing, including the outcome and duration. Log validation errors.
*   **Security-Related Modules (e.g., Access Control):**
    *   Detailed audit logs for every authentication and authorization decision.
    *   Log changes to permissions or security configurations.

This Logging and Auditing Strategy aims to provide comprehensive visibility into the system's operation, facilitate troubleshooting, support security analysis, and meet compliance obligations. It should be reviewed and updated periodically as the system evolves.
