# Error Handling and Retry Strategy

This document outlines strategies for handling errors and implementing retries across different modules of the system. The goal is to build a resilient system that can gracefully handle transient issues while providing clear feedback for permanent failures.

## 1. General Principles

*   **Idempotency:** Design operations to be idempotent wherever possible. Retrying an idempotent operation multiple times will produce the same result as if it were performed once, without unintended side effects. This is crucial for safe retries.
*   **Fail Fast vs. Retry:**
    *   **Retryable Errors:** Errors that are likely transient (e.g., temporary network glitches, service unavailability, rate limits, optimistic locking failures, temporary resource exhaustion). These are candidates for retries.
    *   **Non-Retryable (Fail Fast) Errors:** Errors indicating a permanent issue or invalid state (e.g., invalid input data, authentication/authorization failures, business rule violations, critical bugs). These should generally not be retried automatically for the same request.
*   **Configurable Retry Logic:**
    *   **Max Attempts:** Define a maximum number of retry attempts for a given operation.
    *   **Backoff Strategy:** Implement configurable backoff strategies between retries:
        *   **Exponential Backoff:** Increase the delay between retries exponentially (e.g., 1s, 2s, 4s, 8s). Often combined with jitter (randomness) to avoid thundering herd problems.
        *   **Fixed Delay:** Use a constant delay between retries.
        *   **Custom Intervals:** Define specific intervals for retries.
    *   **Timeouts:** Set timeouts for individual attempts and for the overall retry process.
*   **Dead Letter Queues (DLQs) / Exception Handling:**
    *   For operations processed via message queues (e.g., RabbitMQ, Kafka), messages that fail repeatedly (after exhausting retries) should be moved to a Dead Letter Queue (DLQ) or an equivalent error/exception queue.
    *   This prevents failing messages from blocking the main queue and allows for offline analysis and manual intervention.
*   **Alerting and Monitoring:**
    *   Monitor retry rates, failure rates, and DLQ depths.
    *   Trigger alerts when retry rates exceed thresholds, when messages are sent to DLQs, or when critical operations fail consistently.
*   **User Feedback / System Response:**
    *   For synchronous operations (e.g., API calls), provide clear error responses to the client if an operation fails after retries.
    *   For asynchronous operations, ensure the system status reflects the failure, allowing for later querying or notification.
*   **Manual Intervention / Resend Interface:**
    *   For critical business processes (e.g., statement generation/delivery), provide an administrative interface or tools to:
        *   View failed messages/jobs (from DLQs or error logs).
        *   Inspect error details and payloads.
        *   Manually trigger a retry or resubmission of a specific message/job.
        *   Potentially allow for minor data correction before retrying (with strict auditing).

## 2. Module-Specific Strategies

### a. AFP Generation Service

*   **Potential Errors:**
    *   Template not found or invalid.
    *   Data source connection errors (database, API).
    *   Invalid input data (missing fields, incorrect format).
    *   Resource errors (fonts, images, overlays not found or inaccessible).
    *   Apache FOP processing errors (XSL-FO validation, rendering issues).
*   **Retry Strategy:**
    *   **Data/Template Errors:** Generally non-retryable. Fail fast and log the error. Requires manual correction of data or template.
    *   **Resource/Connection Errors:** Retryable if potentially transient (e.g., temporary network issue accessing a shared font directory, brief database unavailability). Use exponential backoff with a limited number of attempts (e.g., 3-5).
    *   **FOP Errors:** Most are non-retryable as they usually indicate issues with the XSL-FO or input data.

### b. AFP Conversion Service (Wrapping External Tool)

*   **Potential Errors:**
    *   Input AFP file not found, corrupted, or invalid format.
    *   External conversion tool crashing or unavailable.
    *   License issues for commercial conversion tools.
    *   Missing AFP resources (fonts, overlays) required by the converter.
    *   Unsupported AFP features by the conversion tool.
    *   Timeout if the conversion tool hangs.
*   **Retry Strategy:**
    *   **File/Content Errors:** Non-retryable.
    *   **Tool Availability/Crash:** Retryable with backoff if the tool is expected to recover (e.g., if it's a separate process that might be restarted).
    *   **License Issues:** Retryable if transient (e.g., temporary inability to reach a license server). Non-retryable if license is expired/invalid.
    *   **Resource Issues:** Generally non-retryable unless the resource path was temporarily unavailable.
    *   **Timeouts:** Implement overall timeouts for conversion processes. A timeout itself might trigger a retry if the underlying cause is suspected to be transient load.

### c. Email Sender Service

*   **Potential Errors (SMTP):**
    *   **Temporary (4xx codes):** Network issues, recipient mail server temporarily unavailable, greylisting, mailbox full (temporary).
        *   **Retry:** Implement robust retries with exponential backoff and jitter. Configure max attempts (e.g., 10-15 over 24-48 hours).
    *   **Permanent (5xx codes):** Invalid recipient address, domain does not exist, message rejected by policy (e.g., spam filters), authentication failure with relay.
        *   **Retry:** Do not retry these for the same recipient. Log the failure, update email status, and move to a "failed" or "bounced" category.
*   **Potential Errors (Other):**
    *   DKIM signing failure (e.g., misconfigured key). Non-retryable until configuration is fixed.
    *   Template processing errors for email body. Non-retryable.
    *   Attachment processing errors (e.g., PDF file not found). Non-retryable.
*   **Queueing:** Emails should be processed via a persistent queue. Failed temporary deliveries are requeued. Permanently failed deliveries are moved to a DLQ or marked in the database.

### d. SMSSenderService / AppPushService

*   **Potential Errors:**
    *   Invalid phone number / device token (often a permanent error from gateway).
    *   Gateway API errors (HTTP 4xx/5xx from Twilio, FCM, APNs).
    *   Rate limiting by the gateway.
    *   Network connectivity issues to the gateway.
    *   Authentication failure with the gateway.
*   **Retry Strategy:**
    *   **Invalid Recipient/Token:** Non-retryable. Mark as failed and potentially flag the token/number as invalid in `CustomerProfileService`.
    *   **Gateway Temporary Errors (e.g., HTTP 503, some 4xx like rate limits):** Retryable with exponential backoff. Respect `Retry-After` headers if provided by the gateway.
    *   **Gateway Permanent Errors (e.g., HTTP 401 Unauthorized, 400 Bad Request for malformed payload):** Non-retryable. Log and investigate.
    *   **Network Issues:** Retryable.

### e. RESTful APIs (External and Internal)

*   **Error Responses:** Use standard HTTP status codes (4xx for client-side, 5xx for server-side errors) and consistent JSON error bodies.
*   **Client-Side Retries:** API clients (external systems or internal services acting as clients) should implement their own retry logic for:
    *   `5xx` server errors (indicating a problem on the server side that might be transient).
    *   `429 Too Many Requests` (respect `Retry-After` header if present).
    *   Idempotent `POST`/`PUT`/`DELETE` requests if a network error occurs and the outcome is unknown. Use unique idempotency keys if supported by the API.
*   **Server-Side:** Server-side retry logic should be cautious for synchronous API calls to avoid long hold times for clients. Prefer asynchronous processing for operations that involve longer retries.

### f. Batch Processing (e.g., Airflow Orchestrator)

*   **Airflow Built-in Retries:** Airflow tasks support configurable retries with delays (fixed, exponential). Utilize this for tasks that call external services or perform I/O.
*   **Idempotent Tasks:** Design Airflow tasks to be idempotent so they can be safely retried.
*   **DAG-Level Retries:** Consider strategies for retrying entire DAG runs or sections of a DAG if that makes sense for the business process.
*   **Failure Handling:** Define clear `on_failure_callback` tasks in Airflow to handle failures, send notifications, or trigger cleanup processes.

### g. Database Interactions

*   **Transient Errors:** Deadlocks, connection timeouts, temporary unavailability during failover.
*   **Retry Strategy:**
    *   Many ORMs (Object-Relational Mappers) and database connection pools offer built-in retry mechanisms for transient errors. Configure these appropriately.
    *   Implement application-level retries for specific database operations if not handled by the framework, using backoff strategies.
    *   Ensure retries for write operations are handled carefully, especially if not fully idempotent.

## 3. Manual Resend/Reprocess Interface

A crucial part of a robust error handling strategy for critical communications like statements is the ability for operational staff to intervene.

*   **Interface Requirements (Admin Panel or Operational Tool):**
    *   **View Failed Items:** List messages/jobs from DLQs or a database table of processing failures. Display relevant data (e.g., `job_id`, `customer_id`, `communication_type`, error message, timestamp).
    *   **Inspect Details:** Allow drilling down to see the full payload and detailed error history for a failed item.
    *   **Trigger Retry:** Option to manually resubmit a selected item for processing. This might involve placing it back on the original queue or a high-priority retry queue.
    *   **Bulk Retry:** Ability to select multiple failed items and retry them.
    *   **Edit and Retry (Use with extreme caution):** For certain types of failures (e.g., correctable data issue in payload, incorrect recipient detail that has since been fixed), an authorized user might be allowed to edit parts of the payload before retrying. This requires very strict auditing and authorization.
    *   **Mark as Resolved/Archived:** Option to acknowledge a failure that won't be retried and move it out of the active failure list.
*   **Auditing:** All manual interventions (retries, edits) must be strictly audited, logging who performed the action, when, and what was changed.

This strategy will be refined as individual modules are implemented and specific error scenarios are encountered. Configuration for retry attempts, backoff intervals, and timeouts will be externalized as per `Configuration_Management.md`.
