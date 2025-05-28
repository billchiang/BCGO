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

---
## 正體中文 (Traditional Chinese)

# 錯誤處理與重試策略

本文件概述了在系統不同模組中處理錯誤和實作重試的策略。目標是建立一個具備容錯能力的系統，能夠優雅地處理暫時性問題，同時為永久性故障提供明確的回饋。

## 1. 一般原則

*   **冪等性：** 盡可能將操作設計為冪等的。多次重試冪等操作將產生與執行一次相同的結果，而不會產生意外的副作用。這對於安全的重試至關重要。
*   **快速失敗與重試：**
    *   **可重試錯誤：** 可能為暫時性的錯誤 (例如，暫時的網路故障、服務不可用、速率限制、樂觀鎖定失敗、暫時性資源耗盡)。這些是重試的候選項。
    *   **不可重試 (快速失敗) 錯誤：** 表示永久性問題或無效狀態的錯誤 (例如，無效的輸入資料、驗證/授權失敗、業務規則違規、嚴重錯誤)。通常不應為同一請求自動重試這些錯誤。
*   **可設定的重試邏輯：**
    *   **最大嘗試次數：** 為給定操作定義最大重試嘗試次數。
    *   **退避策略：** 在重試之間實作可設定的退避策略：
        *   **指數退避：** 以指數方式增加重試之間的延遲 (例如 1 秒、2 秒、4 秒、8 秒)。通常與抖動 (隨機性) 結合使用，以避免「驚群」問題。
        *   **固定延遲：** 在重試之間使用固定的延遲。
        *   **自訂間隔：** 為重試定義特定的間隔。
    *   **逾時：** 為個別嘗試和整體重試程序設定逾時。
*   **無效信件佇列 (DLQ) / 例外處理：**
    *   對於透過訊息佇列 (例如 RabbitMQ、Kafka) 處理的操作，重複失敗 (在用盡重試次數後) 的訊息應移至無效信件佇列 (DLQ) 或等效的錯誤/例外佇列。
    *   這樣可以防止失敗的訊息阻塞主佇列，並允許離線分析和手動介入。
*   **警示與監控：**
    *   監控重試率、失敗率和 DLQ 深度。
    *   當重試率超過閾值、訊息傳送到 DLQ 或關鍵操作持續失敗時觸發警示。
*   **使用者回饋 / 系統回應：**
    *   對於同步操作 (例如 API 呼叫)，如果在重試後操作失敗，則向用戶端提供明確的錯誤回應。
    *   對於非同步操作，確保系統狀態反映失敗，以便稍後查詢或通知。
*   **手動介入 / 重新傳送介面：**
    *   對於關鍵業務流程 (例如帳單產生/交付)，提供管理介面或工具以：
        *   檢視失敗的訊息/工作 (來自 DLQ 或錯誤日誌)。
        *   檢查錯誤詳細資訊和負載。
        *   手動觸發特定訊息/工作的重試或重新提交。
        *   在重試前可能允許進行次要資料更正 (需嚴格稽核)。

## 2. 特定模組策略

### a. AFP 產生服務

*   **潛在錯誤：**
    *   找不到範本或範本無效。
    *   資料來源連線錯誤 (資料庫、API)。
    *   輸入資料無效 (缺少欄位、格式不正確)。
    *   資源錯誤 (找不到字型、影像、疊加層或無法存取)。
    *   Apache FOP 處理錯誤 (XSL-FO 驗證、呈現問題)。
*   **重試策略：**
    *   **資料/範本錯誤：** 通常不可重試。快速失敗並記錄錯誤。需要手動更正資料或範本。
    *   **資源/連線錯誤：** 如果可能是暫時性的 (例如，存取共用字型目錄時發生暫時性網路問題、資料庫短暫無法使用)，則可重試。使用指數退避並限制嘗試次數 (例如 3-5 次)。
    *   **FOP 錯誤：** 大多數不可重試，因為它們通常表示 XSL-FO 或輸入資料的問題。

### b. AFP 轉換服務 (包裝外部工具)

*   **潛在錯誤：**
    *   找不到輸入 AFP 檔案、檔案損毀或格式無效。
    *   外部轉換工具當機或無法使用。
    *   商業轉換工具的授權問題。
    *   轉換器缺少所需的 AFP 資源 (字型、疊加層)。
    *   轉換工具不支援的 AFP 功能。
    *   如果轉換工具掛起，則逾時。
*   **重試策略：**
    *   **檔案/內容錯誤：** 不可重試。
    *   **工具可用性/當機：** 如果工具預期會復原 (例如，如果它是可能重新啟動的單獨程序)，則可使用退避重試。
    *   **授權問題：** 如果是暫時性的 (例如，暫時無法連線到授權伺服器)，則可重試。如果授權已過期/無效，則不可重試。
    *   **資源問題：** 通常不可重試，除非資源路徑暫時無法使用。
    *   **逾時：** 為轉換程序實作整體逾時。如果懷疑根本原因是暫時性負載，則逾時本身可能會觸發重試。

### c. 電子郵件傳送服務

*   **潛在錯誤 (SMTP)：**
    *   **暫時性 (4xx 代碼)：** 網路問題、收件人郵件伺服器暫時無法使用、灰名單、信箱已滿 (暫時性)。
        *   **重試：** 實作具有指數退避和抖動的強大重試。設定最大嘗試次數 (例如，在 24-48 小時內進行 10-15 次)。
    *   **永久性 (5xx 代碼)：** 收件人地址無效、網域不存在、訊息遭原則拒絕 (例如垃圾郵件篩選器)、與中繼伺服器驗證失敗。
        *   **重試：** 請勿對同一收件人重試這些錯誤。記錄失敗，更新電子郵件狀態，並移至「失敗」或「退信」類別。
*   **潛在錯誤 (其他)：**
    *   DKIM 簽署失敗 (例如金鑰設定錯誤)。在修正組態之前不可重試。
    *   電子郵件內文的範本處理錯誤。不可重試。
    *   附件處理錯誤 (例如找不到 PDF 檔案)。不可重試。
*   **佇列：** 電子郵件應透過永續性佇列處理。暫時傳送失敗的郵件會重新排入佇列。永久傳送失敗的郵件會移至 DLQ 或在資料庫中標記。

### d. SMSSenderService / AppPushService

*   **潛在錯誤：**
    *   電話號碼/裝置權杖無效 (通常是來自閘道的永久性錯誤)。
    *   閘道 API 錯誤 (來自 Twilio、FCM、APNs 的 HTTP 4xx/5xx)。
    *   閘道的速率限制。
    *   與閘道的網路連線問題。
    *   與閘道驗證失敗。
*   **重試策略：**
    *   **無效的收件人/權杖：** 不可重試。標記為失敗，並可能在 `CustomerProfileService` 中將權杖/號碼標記為無效。
    *   **閘道暫時性錯誤 (例如 HTTP 503、某些 4xx 如速率限制)：** 可使用指數退避重試。如果閘道提供，請遵循 `Retry-After` 標頭。
    *   **閘道永久性錯誤 (例如 HTTP 401 未經授權、格式錯誤負載的 400 錯誤請求)：** 不可重試。記錄並調查。
    *   **網路問題：** 可重試。

### e. RESTful API (外部與內部)

*   **錯誤回應：** 使用標準 HTTP 狀態碼 (用戶端錯誤為 4xx，伺服器端錯誤為 5xx) 和一致的 JSON 錯誤內文。
*   **用戶端重試：** API 用戶端 (外部系統或充當用戶端的內部服務) 應針對以下情況實作自己的重試邏輯：
    *   `5xx` 伺服器錯誤 (表示伺服器端可能存在暫時性問題)。
    *   `429 Too Many Requests` (如果存在，請遵循 `Retry-After` 標頭)。
    *   如果發生網路錯誤且結果未知，則為冪等的 `POST`/`PUT`/`DELETE` 請求。如果 API 支援，請使用唯一的冪等性金鑰。
*   **伺服器端：** 伺服器端重試邏輯應謹慎處理同步 API 呼叫，以避免用戶端長時間等待。對於涉及較長重試的操作，建議使用非同步處理。

### f. 批次處理 (例如 Airflow 協調器)

*   **Airflow 內建重試：** Airflow 工作支援可設定的重試和延遲 (固定、指數)。將此用於呼叫外部服務或執行 I/O 的工作。
*   **冪等工作：** 將 Airflow 工作設計為冪等的，以便可以安全地重試。
*   **DAG 層級重試：** 如果對於業務流程有意義，請考慮重試整個 DAG 執行或 DAG 的某些部分的策略。
*   **失敗處理：** 在 Airflow 中定義明確的 `on_failure_callback` 工作，以處理失敗、傳送通知或觸發清除程序。

### g. 資料庫互動

*   **暫時性錯誤：** 死鎖、連線逾時、容錯移轉期間暫時無法使用。
*   **重試策略：**
    *   許多 ORM (物件關聯對應器) 和資料庫連線池為暫時性錯誤提供內建的重試機制。適當設定這些機制。
    *   如果框架未處理，則為特定資料庫操作實作應用程式層級的重試，並使用退避策略。
    *   確保仔細處理寫入操作的重試，尤其是在不完全冪等的情況下。

## 3. 手動重新傳送/重新處理介面

對於像帳單這樣重要的通訊，健全的錯誤處理策略的一個關鍵部分是營運人員能夠介入。

*   **介面需求 (管理面板或營運工具)：**
    *   **檢視失敗項目：** 列出 DLQ 或處理失敗資料庫資料表中的訊息/工作。顯示相關資料 (例如 `job_id`、`customer_id`、`communication_type`、錯誤訊息、時間戳記)。
    *   **檢查詳細資訊：** 允許深入查看失敗項目的完整負載和詳細錯誤歷史記錄。
    *   **觸發重試：** 手動重新提交所選項目進行處理的選項。這可能涉及將其放回原始佇列或高優先順序的重試佇列。
    *   **批次重試：** 能夠選擇多個失敗項目並重試它們。
    *   **編輯並重試 (極度謹慎使用)：** 對於某些類型的失敗 (例如負載中可更正的資料問題、此後已修正的錯誤收件人詳細資訊)，可能允許授權使用者在重試前編輯負載的某些部分。這需要非常嚴格的稽核和授權。
    *   **標記為已解決/已封存：** 確認不會重試的失敗並將其從作用中失敗清單中移除的選項。
*   **稽核：** 所有手動介入 (重試、編輯) 都必須受到嚴格稽核，記錄執行動作的人員、時間以及變更的內容。

隨著個別模組的實作和遇到特定的錯誤情境，此策略將會不斷完善。重試嘗試次數、退避間隔和逾時的組態將根據 `Configuration_Management.md` 進行外部化。
