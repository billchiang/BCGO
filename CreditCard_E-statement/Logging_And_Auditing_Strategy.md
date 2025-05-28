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

---
## 正體中文 (Traditional Chinese)

# 日誌記錄與稽核策略

本文件定義了信用卡電子帳單系統所有模組中記錄應用程式事件、錯誤和安全性相關稽核追蹤的統一策略。有效的日誌記錄和稽核對於監控系統健康狀況、疑難排解問題、安全性分析和滿足合規性要求至關重要。

## 1. 一般日誌記錄原則

*   **結構化日誌記錄：**
    *   **格式：** 所有應用程式日誌必須採用結構化格式，最好是 JSON。每個日誌項目應為單一 JSON 物件。
    *   **優點：** 便於在集中式日誌記錄系統中進行剖析、篩選、搜尋和分析。允許一致的欄位名稱和資料類型。
*   **日誌層級：**
    *   必須使用標準日誌層級 (DEBUG、INFO、WARN、ERROR、CRITICAL/FATAL)。
    *   **DEBUG：** 對於開發人員在疑難排解期間有用的細微性資訊。預設情況下應在生產環境中停用。
    *   **INFO：** 關於正常應用程式行為和重要生命週期事件的高階資訊 (例如服務啟動、請求處理、批次工作完成)。
    *   **WARN：** 表示潛在問題或非預期情況，這些情況尚非嚴重錯誤，但如果不加以解決可能會導致問題 (例如已棄用的 API 使用、資源接近容量)。
    *   **ERROR：** 阻止特定操作完成但未必會關閉服務的錯誤。應用程式通常可以復原或繼續執行其他操作。
    *   **CRITICAL/FATAL：** 導致應用程式或關鍵元件終止或變得不穩定的嚴重錯誤。
    *   日誌層級必須可針對每個環境和每個服務進行設定。生產環境應預設為 INFO 或 WARN。
*   **關聯 ID：**
    *   必須在請求的進入點 (例如 API 閘道、初始工作提交) 產生唯一的關聯 ID (例如 `trace_id`、`request_id`)，或者如果存在，則從傳入請求標頭中擷取。
    *   此 ID 必須在所有微服務呼叫中傳播，並包含在與該特定請求或交易相關的每個日誌項目中。這允許追蹤跨分散式服務的作業的整個生命週期。
*   **時間戳記：**
    *   所有日誌項目都必須包含一個準確、高精確度的時間戳記，指示事件發生的時間。
    *   時間戳記應採用 UTC 格式以避免時區模糊。範例格式：ISO 8601 (`YYYY-MM-DDTHH:mm:ss.sssZ`)。
*   **內容相關資訊：**
    *   日誌項目必須包含相關的內容相關資訊，以協助理解和偵錯。常見的內容欄位包括：
        *   `service_name`：產生記錄的微服務名稱。
        *   `service_version`：服務的版本。
        *   `hostname` / `instance_id`：執行服務之特定執行個體的識別碼。
        *   `thread_id`：(如果適用)
        *   `user_id`：起始動作之使用者的識別碼 (如果適用且可用)。
        *   `customer_id`：相關的客戶識別碼。
        *   `job_id` / `batch_id`：批次程序或特定工作的識別碼。
        *   `class_name` / `method_name`：程式碼中日誌的來源。
*   **日誌中敏感資料的處理：**
    *   嚴格避免以純文字記錄敏感的個人識別資訊 (PII) (例如完整的信用卡號碼、銀行帳戶詳細資訊、國民身分證號碼、完整的 AFP/PDF 內容、密碼、API 金鑰、存取權杖)。
    *   如果偵錯需要，敏感資料必須進行遮罩 (例如僅顯示帳號末 4 碼) 或權杖化。
    *   建立關於哪些構成敏感資料以及應如何在日誌中處理這些資料的明確指導方針。
    *   定期檢閱日誌內容以確保符合此原則。

## 2. 日誌類型

*   **事件日誌 (應用程式日誌)：**
    *   記錄應用程式生命週期和業務邏輯執行中的重要非錯誤事件。
    *   **範例：**
        *   服務啟動和關閉順序。
        *   收到 API 請求和傳送回應 (摘要，除非特別需要且經過清理，否則不包含完整內文)。
        *   從佇列取用訊息或發佈到佇列的訊息。
        *   成功產生 AFP、轉換 PDF、組合電子郵件。
        *   電子郵件已分派到 SMTP 伺服器，簡訊已傳送到閘道。
        *   批次工作已啟動、進度檢查點、成功完成。
        *   關鍵業務流程里程碑。
*   **錯誤日誌：**
    *   記錄應用程式內發生的所有錯誤和例外狀況。
    *   必須包含例外的完整堆疊追蹤。
    *   必須包含相關的內容相關資訊 (關聯 ID、如果安全則包含輸入參數、目前狀態) 以協助診斷錯誤。
    *   區分已處理的例外狀況 (應用程式从中復原) 和未處理的例外狀況 (可能導致服務終止)。
*   **存取日誌：**
    *   通常由 Web 伺服器、API 閘道或負載平衡器產生。
    *   記錄所有傳入的 HTTP 請求，包括：來源 IP 位址、時間戳記、HTTP 方法、請求的 URL/路徑、HTTP 狀態碼、回應大小、回應時間、使用者代理程式、參照位址。
    *   這些對於流量分析、安全性監控和識別惡意活動至關重要。
*   **偵錯日誌：**
    *   為開發人員在疑難排解期間提供詳細的診斷資訊。
    *   可能包括變數值、詳細的逐步執行路徑。
    *   由於數量龐大且可能影響效能，預設情況下應在生產環境中停用。如果即時偵錯需要，可以針對特定元件或請求動態啟用。

## 3. 稽核追蹤 (稽核日誌)

*   **目的：**
    *   提供使用者或系統執行的安全性相關事件和動作的時間順序記錄。
    *   對於遵守法規 (例如 GDPR、SOX、HIPAA (如果適用))、安全性分析、鑑識調查和建立不可否認性至關重要。
    *   稽核日誌必須受到保護，防止未經授權的修改或刪除 (防竄改或抗竄改儲存)。
*   **要稽核的關鍵事件：**
    *   **驗證與授權事件：**
        *   使用者登入嘗試 (成功、失敗、來源 IP)。
        *   使用者登出。
        *   API 用戶端驗證嘗試 (成功、失敗)。
        *   密碼變更、MFA 狀態變更/註冊。
        *   工作階段建立/失效。
    *   **存取控制變更：**
        *   使用者帳戶的建立、修改、刪除。
        *   角色和權限的指派或撤銷。
        *   存取控制原則的變更。
    *   **資源生命週期與管理：**
        *   **範本：** AFP、電子郵件、SMS、推播範本的建立、修改、刪除、啟用/停用。
        *   **通訊工作：** 提交、取消、手動重試/重新傳送動作 (包括誰起始動作、如果有的話變更了什麼以及何時變更)。
        *   **客戶偏好設定：** 客戶通訊偏好設定、聯絡詳細資訊和同意狀態的變更 (包括變更來源)。
    *   **敏感資料存取 (如果適用)：**
        *   使用者或系統明確檢視或匯出敏感資料的任何動作 (例如管理員檢視詳細 PII 或完整帳單內容，如果存在此類功能且允許)。
    *   **系統組態變更：**
        *   對關鍵系統組態的修改 (例如 SMTP 伺服器設定、API 閘道原則、安全性設定、日誌層級、保留原則)。
    *   **安全性系統事件：**
        *   從 WAF、IDS/IPS 偵測到的安全性警示。
        *   執行與安全性相關的管理工作。
        *   加密金鑰狀態的變更 (如果由應用程式管理)。
*   **稽核日誌內容：**
    *   **時間戳記 (UTC)：** 事件的精確時間。
    *   **執行者/主體：** 執行動作的使用者 ID、服務帳戶 ID 或系統程序。
    *   **動作：** 執行動作的描述 (例如 "USER_LOGIN_SUCCESS"、"TEMPLATE_CREATED"、"MANUAL_EMAIL_RESEND")。
    *   **目標/物件：** 執行動作的資源或實體 (例如範本 ID、客戶 ID、組態設定名稱)。
    *   **結果：** 動作的成功或失敗。
    *   **來源 IP 位址：** 動作源自的 IP 位址。
    *   **其他詳細資訊：** 任何相關參數或內容 (例如變更的值、如果提供則為動作原因)。
*   **保護與保留：** 稽核日誌應安全儲存，並具有嚴格的存取控制，並根據法律和業務要求進行保留 (請參閱 `Data_Retention_And_Archival_Policy.md`)。如果可能，請考慮一次寫入多次讀取 (WORM) 儲存特性。

## 4. 日誌記錄基礎架構與工具

本節整合並參考 `Technology_Stack_and_Infrastructure.md` 中所做的選擇。

*   **日誌收集：**
    *   在每個微服務中使用標準化的日誌記錄函式庫 (例如 Java 的 SLF4j 搭配 Logback/Log4j2、Python 的 `logging` 模組、.NET 的 Serilog/NLog)。
    *   容器日誌 (stdout/stderr) 將由容器協同運作平台 (Kubernetes) 收集。
    *   可以使用專用的日誌傳送代理程式 (例如 Filebeat、Fluentd、雲端供應商代理程式，如 CloudWatch Agent、Azure Monitor Agent) 從各種來源收集日誌。
*   **日誌聚合、儲存與處理：**
    *   **ELK Stack (Elasticsearch、Logstash、Kibana)：**
        *   **Elasticsearch：** 用於儲存、索引和搜尋大量日誌資料。
        *   **Logstash (或 Fluentd)：** 用於在將日誌傳送到 Elasticsearch 之前收集、剖析、轉換和充實日誌。
        *   **Kibana：** 用於視覺化、探索和建立日誌資料儀表板。
    *   **Grafana Loki：** 一個輕量級、受 Prometheus 啟發的日誌記錄後端。適用於已使用 Grafana 進行指標監控的環境。
    *   選擇取決於營運熟悉度、規模和功能需求。
*   **日誌格式標準化：** 所有服務都必須以商定的結構化 JSON 格式發出日誌，以確保在集中式系統中進行一致的剖析和索引。
*   **日誌保留：** 日誌保留原則 (熱儲存與冷/封存儲存) 將在 `Data_Retention_And_Archival_Policy.md` 中定義。
*   **日誌監控與警示：**
    *   利用所選日誌記錄堆疊中的功能 (例如 ELK 的 ElastAlert、Loki 的 Grafana 警示) 根據特定的日誌模式、錯誤計數或安全性事件 ID 建立警示。
    *   範例：針對高頻率的 ERROR/CRITICAL 訊息、多次失敗的登入嘗試、特定的安全性事件特徵發出警示。

## 5. 特定模組的日誌記錄考量

在遵守一般原則的同時，特定模組可能有獨特的日誌記錄需求：

*   **核心 AFP 產生引擎：**
    *   記錄使用的範本 ID、輸入資料的來源 (例如請求 ID、批次 ID，除非特別需要且經過遮罩，否則不記錄完整資料本身)。
    *   記錄產生的成功/失敗、花費的時間以及來自 Apache FOP 的任何錯誤。
    *   記錄 AFP 資源 (字型、疊加層、影像) 的使用情況。
*   **AFP 轉換模組：**
    *   記錄輸入 AFP 檔案識別碼、目標轉換格式。
    *   記錄所叫用的外部轉換工具的詳細資訊 (如果為 CLI，則記錄指令、參數)。
    *   記錄轉換的成功/失敗、花費的時間以及來自外部工具的任何錯誤訊息或結束代碼。
*   **電子郵件/簡訊/應用程式推播傳送服務：**
    *   記錄唯一的訊息 ID、收件人識別碼 (如果是 PII 且對於日誌項目的目的而言並非必要，則進行遮罩)。
    *   記錄與閘道/SMTP 伺服器的互動 (例如已傳送請求、已收到回應、狀態更新/回呼)。
    *   記錄詳細的傳送狀態 (已排入佇列、已傳送、失敗、退信、已送達 (如果可用))。
    *   記錄與訊息組合、DKIM 簽署或閘道通訊相關的錯誤。
*   **批次處理 (Airflow/協調器)：**
    *   Airflow 為 DAG 和工作執行提供廣泛的日誌記錄。這些日誌應由集中式日誌記錄系統擷取。
    *   記錄每個批次執行的關鍵參數、要處理的項目數、進度和最終結果 (成功、失敗、部分成功)。
*   **RESTful API 模組 (API 閘道與服務)：**
    *   API 閘道將提供詳細的存取日誌。
    *   後端服務應記錄 API 請求處理的開始和結束，包括結果和持續時間。記錄驗證錯誤。
*   **安全性相關模組 (例如存取控制)：**
    *   針對每個驗證和授權決策提供詳細的稽核日誌。
    *   記錄對權限或安全性組態的變更。

此日誌記錄與稽核策略旨在提供對系統營運的全面可見性、促進疑難排解、支援安全性分析並滿足合規性義務。隨著系統的發展，應定期檢閱和更新此策略。
