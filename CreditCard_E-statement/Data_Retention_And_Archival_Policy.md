# Data Retention and Archival Policy

This document outlines the policy for data retention, archival, and disposal for the CreditCard E-statement system.

## 1. Introduction & Goals

*   **Purpose:** To establish clear guidelines for how long different categories of data generated and processed by the system are retained, how they are archived for long-term storage, and how they are securely disposed of when no longer needed.
*   **Goals:**
    *   Comply with applicable legal, regulatory, and contractual data retention requirements.
    *   Manage storage costs effectively by moving older, less frequently accessed data to cheaper storage tiers or deleting it.
    *   Optimize system performance by reducing the volume of data in active operational systems.
    *   Ensure data is available for required periods for business operations, customer service, auditing, and legal purposes.
    *   Minimize risks associated with storing excessive or outdated data.
*   **Scope:** This policy applies to all data categories within the system, including but not limited to:
    *   Customer account information and preferences.
    *   Generated AFP documents and converted PDF statements.
    *   Communication records (email, SMS, push notification metadata and potentially content).
    *   Application logs (event, error, debug logs).
    *   Audit logs.
    *   Batch job history and metadata.
    *   Temporary files.

## 2. Data Categories and Retention Periods

Retention periods are driven by business needs and legal/regulatory obligations. The following are initial guidelines and MUST be reviewed and approved by legal and compliance stakeholders. "X, Y, Z, A, B, C, D, E, F, G, H" are placeholders for specific durations (e.g., "30 days", "1 year", "7 years").

*   **Customer Account Data (managed by `CustomerProfileService`):**
    *   **Active Customer Profile Data (including contact details, preferences):** Retain as long as the customer's account is active and for **X_AccountData years** after account closure or inactivity.
    *   **Consent Records (for communication preferences, GDPR/PDPA):** Retain for **Y_Consent years** after the consent is withdrawn or the associated account is closed, to provide an audit trail of consent.
*   **Generated Statement Documents (AFP, PDF):**
    *   **Hot Storage (Online, readily accessible via API for download):** Retain for **Z_DocHot months** from the statement date (e.g., 12-36 months).
    *   **Cold Storage (Archival, for long-term retention):** After Z months, move to cold storage. Retain in cold storage until **W_DocTotal years** from the statement date (e.g., 7-10 years total, aligned with financial record-keeping regulations).
    *   **Purge:** Securely delete from cold storage after W years.
*   **Communication Records (Metadata for Email, SMS, App Push: e.g., recipient, timestamp, status - `sent`, `delivered`, `bounced`, `job_id`):**
    *   **Hot Storage (Operational Database):** Retain for **A_CommHot months** (e.g., 6-18 months) for operational tracking, resend capabilities, and immediate customer service inquiries.
    *   **Cold Storage (Archival):** After A months, metadata can be archived. Retain in cold storage for **B_CommTotal years** for historical analysis and audit purposes.
    *   **Communication Content (e.g., email body, SMS text - if stored separately from metadata):**
        *   Transactional message content (if deemed part of the official statement/record): May follow the same retention as "Generated Statement Documents."
        *   Marketing message content: Shorter retention, e.g., **B_MarketingContent months**.
*   **Application Logs (Event, Error, Debug Logs from microservices):**
    *   **Hot Storage (Centralized Logging System like ELK/Loki):** Retain for **C_AppLogHot days** (e.g., 30-90 days) for real-time monitoring, troubleshooting, and operational diagnostics.
    *   **Cold Storage (Archival):** After C days, logs can be archived. Retain in cold storage for **D_AppLogArchive months/years** (e.g., 1-2 years) for trend analysis or deeper forensic investigation if required.
*   **Audit Logs (Security-relevant events, user actions, system changes):**
    *   **Hot Storage (Centralized Logging System, potentially with stricter access):** Retain for **E_AuditLogHot months** (e.g., 6-12 months) for active security monitoring and immediate investigation.
    *   **Cold Storage (Archival):** After E months, audit logs MUST be archived. Retain in cold storage for **F_AuditLogArchive years** (e.g., 7-10 years or longer, as per specific compliance mandates like SOX, GDPR). Audit logs often have the longest retention periods.
*   **Batch Job History (e.g., Airflow metadata, job status, parameters):**
    *   **Hot Storage (Operational Database of Orchestrator):** Retain detailed job run history for **G_BatchHot months** (e.g., 3-6 months) for operational monitoring and immediate troubleshooting.
    *   **Cold Storage (Archival):** Summary data (job name, execution time, status, number of items processed) can be archived for **H_BatchArchive years** for trend analysis and capacity planning. Detailed logs follow application log retention.
*   **Temporary Files:**
    *   Files generated during intermediate processing steps (e.g., temporary data extracts before AFP generation, intermediate files during multi-step conversions) MUST be deleted immediately after the successful completion of the step or job they are related to.
    *   Implement scheduled cleanup jobs to find and delete any orphaned temporary files older than a few days (e.g., >3 days).

## 3. Archival Strategy

*   **Mechanism:**
    *   Automated processes (e.g., scheduled scripts, Airflow DAGs, database procedures, lifecycle policies in cloud storage) will be responsible for moving data from hot (operational) storage to cold (archival) storage based on the defined retention periods.
*   **Cold Storage Solutions:**
    *   **Cloud-based:** AWS S3 Glacier & S3 Glacier Deep Archive, Azure Blob Archive tier, Google Cloud Archive Storage.
    *   **On-Premise:** Tape libraries, optical storage, or dedicated archival disk systems.
    *   The choice depends on cost, retrieval time requirements, and durability needs.
*   **Data Format for Archives:**
    *   Data should be archived in open, durable, and preferably compressed formats (e.g., Parquet or Avro for structured metadata, Gzip/Zstd compressed JSON/text for logs, original PDF/AFP for documents).
    *   Ensure that the format can be read and processed for the entire duration of the archival period.
*   **Encryption of Archives:** All archived data, especially if containing sensitive information, MUST be encrypted at rest using strong encryption algorithms. Manage encryption keys securely.
*   **Accessibility and Retrieval from Archives:**
    *   Define clear procedures for accessing and retrieving data from archives.
    *   Retrieval from cold storage can be slow (minutes to hours) and may incur additional costs. Plan accordingly.
    *   Access to archived data must be strictly controlled and audited.
*   **Index/Catalog for Archives:**
    *   Maintain an index or catalog of archived data (e.g., what data was archived, when, its original identifiers, its location in the archive). This is crucial for efficiently finding and retrieving specific archived data when needed.

## 4. Data Disposal/Purge Strategy

*   **Mechanism:**
    *   Automated processes will be responsible for securely deleting data from both hot and cold storage once its total defined retention period has expired.
    *   Deletion should be permanent and irrecoverable.
*   **Secure Deletion Methods:**
    *   For disk-based storage, use cryptographic erasure (deleting the encryption keys) or multi-pass overwrite methods if required by specific standards for highly sensitive data.
    *   Cloud storage services often provide mechanisms for secure deletion as part of their lifecycle policies.
*   **Verification:** Implement checks or sampling methods to verify that data has been properly and completely deleted according to the policy.
*   **Auditing:** All data disposal actions (automated or manual) MUST be logged in an audit trail, including what data was deleted, when, and by what process/user.

## 5. Legal and Regulatory Compliance

*   **Alignment:** This Data Retention and Archival Policy MUST be reviewed and updated regularly to ensure it aligns with all applicable local, national, and international laws and regulations (e.g., GDPR, CCPA, financial industry data retention laws, tax laws, e-discovery requirements).
*   **Legal Holds:** Implement a documented process for handling legal holds or litigation freezes. If data is subject to a legal hold, its scheduled disposal MUST be suspended until the hold is lifted. This process must be auditable.
*   **Consultation:** Legal and compliance teams MUST be consulted to define and approve the specific retention periods for each data category and to ensure the policy meets all legal obligations.

## 6. Policy Enforcement and Responsibilities

*   **Ownership:** A designated role or team (e.g., Data Governance Officer, System Owner, Compliance Team) will be responsible for the overall ownership, maintenance, and enforcement of this policy.
*   **Automation:** Policy enforcement (archival, deletion) should be automated as much as possible to ensure consistency and reduce manual error.
*   **Monitoring & Auditing:** Regularly audit the implementation of the policy to ensure it is being followed correctly (e.g., verify data is being archived/deleted as scheduled).
*   **Training:** Relevant personnel should be trained on this policy and their responsibilities.

## 7. Exceptions

*   Any exceptions to this policy must be for a valid, documented business or legal reason.
*   All exceptions must be approved by designated authorities (e.g., Data Governance Officer, Legal Department, Senior Management).
*   A record of all approved exceptions must be maintained.

This policy will be reviewed at least annually and updated as necessary to reflect changes in business requirements, technology, and legal/regulatory landscapes.

---
## 正體中文 (Traditional Chinese)

# 資料保留與封存政策

本文件概述了信用卡電子帳單系統的資料保留、封存和處置政策。

## 1. 簡介與目標

*   **目的：** 為系統產生和處理的不同類別資料的保留時間、長期儲存的封存方式以及不再需要時的安全處置方式建立明確的指導方針。
*   **目標：**
    *   遵守適用的法律、法規和合約資料保留要求。
    *   透過將較舊、較不常存取的資料移至較便宜的儲存層或將其刪除，有效管理儲存成本。
    *   透過減少作用中營運系統中的資料量來最佳化系統效能。
    *   確保資料在業務營運、客戶服務、稽核和法律用途所需的期間內可用。
    *   最小化與儲存過多或過期資料相關的風險。
*   **範圍：** 此政策適用於系統內的所有資料類別，包括但不限於：
    *   客戶帳戶資訊和偏好設定。
    *   產生的 AFP 文件和轉換後的 PDF 帳單。
    *   通訊記錄 (電子郵件、簡訊、推播通知中繼資料以及可能的內容)。
    *   應用程式日誌 (事件、錯誤、偵錯日誌)。
    *   稽核日誌。
    *   批次工作歷史記錄和中繼資料。
    *   暫存檔案。

## 2. 資料類別與保留期間

保留期間取決於業務需求和法律/法規義務。以下為初步指導方針，必須由法律和合規利害關係人檢閱並核准。「X、Y、Z、A、B、C、D、E、F、G、H」為特定持續時間的預留位置 (例如「30 天」、「1 年」、「7 年」)。

*   **客戶帳戶資料 (由 `CustomerProfileService` 管理)：**
    *   **作用中客戶設定檔資料 (包括聯絡方式、偏好設定)：** 在客戶帳戶有效期間以及帳戶關閉或停用後保留 **X_AccountData 年**。
    *   **同意記錄 (用於通訊偏好設定、GDPR/PDPA)：** 在同意撤銷或相關帳戶關閉後保留 **Y_Consent 年**，以提供同意的稽核追蹤。
*   **產生的帳單文件 (AFP、PDF)：**
    *   **熱儲存 (線上，可透過 API 輕鬆存取以下載)：** 從帳單日期起保留 **Z_DocHot 個月** (例如 12-36 個月)。
    *   **冷儲存 (封存，用於長期保留)：** Z 個月後，移至冷儲存。在冷儲存中保留至帳單日期起 **W_DocTotal 年** (例如總共 7-10 年，與財務記錄保存法規一致)。
    *   **清除：** W 年後從冷儲存中安全刪除。
*   **通訊記錄 (電子郵件、簡訊、應用程式推播的中繼資料：例如收件人、時間戳記、狀態 - `sent`、`delivered`、`bounced`、`job_id`)：**
    *   **熱儲存 (營運資料庫)：** 保留 **A_CommHot 個月** (例如 6-18 個月)，用於營運追蹤、重新傳送功能和即時客戶服務查詢。
    *   **冷儲存 (封存)：** A 個月後，可以封存中繼資料。在冷儲存中保留 **B_CommTotal 年**，用於歷史分析和稽核目的。
    *   **通訊內容 (例如電子郵件內文、簡訊文字 - 如果與中繼資料分開儲存)：**
        *   交易訊息內容 (如果被視為官方帳單/記錄的一部分)：可能遵循與「產生的帳單文件」相同的保留期間。
        *   行銷訊息內容：較短的保留期間，例如 **B_MarketingContent 個月**。
*   **應用程式日誌 (來自微服務的事件、錯誤、偵錯日誌)：**
    *   **熱儲存 (集中式日誌記錄系統，如 ELK/Loki)：** 保留 **C_AppLogHot 天** (例如 30-90 天)，用於即時監控、疑難排解和營運診斷。
    *   **冷儲存 (封存)：** C 天後，可以封存日誌。在冷儲存中保留 **D_AppLogArchive 個月/年** (例如 1-2 年)，用於趨勢分析或在需要時進行更深入的鑑識調查。
*   **稽核日誌 (安全性相關事件、使用者動作、系統變更)：**
    *   **熱儲存 (集中式日誌記錄系統，可能具有更嚴格的存取權限)：** 保留 **E_AuditLogHot 個月** (例如 6-12 個月)，用於主動安全性監控和即時調查。
    *   **冷儲存 (封存)：** E 個月後，必須封存稽核日誌。在冷儲存中保留 **F_AuditLogArchive 年** (例如 7-10 年或更長，根據特定合規性要求，如 SOX、GDPR)。稽核日誌通常具有最長的保留期間。
*   **批次工作歷史記錄 (例如 Airflow 中繼資料、工作狀態、參數)：**
    *   **熱儲存 (協調器的營運資料庫)：** 保留詳細的工作執行歷史記錄 **G_BatchHot 個月** (例如 3-6 個月)，用於營運監控和即時疑難排解。
    *   **冷儲存 (封存)：** 可以封存摘要資料 (工作名稱、執行時間、狀態、已處理項目數) **H_BatchArchive 年**，用於趨勢分析和容量規劃。詳細日誌遵循應用程式日誌保留期間。
*   **暫存檔案：**
    *   在中間處理步驟中產生的檔案 (例如 AFP 產生前的暫存資料擷取、多步驟轉換期間的中間檔案) 必須在其相關步驟或工作成功完成後立即刪除。
    *   實作排程的清除工作，以尋找並刪除任何超過幾天 (例如 >3 天) 的孤立暫存檔案。

## 3. 封存策略

*   **機制：**
    *   自動化程序 (例如排程指令碼、Airflow DAG、資料庫程序、雲端儲存中的生命週期原則) 將負責根據定義的保留期間將資料從熱 (營運) 儲存移至冷 (封存) 儲存。
*   **冷儲存解決方案：**
    *   **雲端型：** AWS S3 Glacier 和 S3 Glacier Deep Archive、Azure Blob 封存層、Google Cloud Archive Storage。
    *   **本地部署：** 磁帶櫃、光學儲存或專用封存磁碟系統。
    *   選擇取決於成本、擷取時間要求和耐用性需求。
*   **封存資料格式：**
    *   資料應以開放、耐用且最好是壓縮的格式封存 (例如，用於結構化中繼資料的 Parquet 或 Avro，用於日誌的 Gzip/Zstd 壓縮 JSON/文字，用於文件的原始 PDF/AFP)。
    *   確保格式可以在整個封存期間讀取和處理。
*   **封存加密：** 所有封存資料，尤其是包含敏感資訊的資料，都必須使用強式加密演算法進行靜態加密。安全地管理加密金鑰。
*   **封存資料的可存取性與擷取：**
    *   定義存取和擷取封存資料的明確程序。
    *   從冷儲存擷取可能很慢 (數分鐘到數小時)，並且可能會產生額外費用。請據此規劃。
    *   對封存資料的存取必須受到嚴格控制和稽核。
*   **封存索引/目錄：**
    *   維護封存資料的索引或目錄 (例如，封存了哪些資料、何時封存、其原始識別碼、其在封存中的位置)。這對於在需要時有效尋找和擷取特定封存資料至關重要。

## 4. 資料處置/清除策略

*   **機制：**
    *   一旦資料的總定義保留期間到期，自動化程序將負責從熱儲存和冷儲存中安全地刪除資料。
    *   刪除應為永久性且不可復原。
*   **安全刪除方法：**
    *   對於基於磁碟的儲存，如果特定標準對高度敏感資料有要求，請使用加密清除 (刪除加密金鑰) 或多遍覆寫方法。
    *   雲端儲存服務通常提供安全刪除機制作為其生命週期原則的一部分。
*   **驗證：** 實作檢查或抽樣方法，以驗證資料是否已根據政策正確且完整地刪除。
*   **稽核：** 所有資料處置動作 (自動或手動) 都必須記錄在稽核追蹤中，包括刪除了哪些資料、何時刪除以及由哪個程序/使用者刪除。

## 5. 法律與法規遵循

*   **一致性：** 此資料保留與封存政策必須定期檢閱和更新，以確保其符合所有適用的地方、國家和國際法律法規 (例如 GDPR、CCPA、金融業資料保留法、稅法、電子蒐證要求)。
*   **法律保留：** 實作處理法律保留或訴訟凍結的書面程序。如果資料受到法律保留的約束，則必須暫停其排程處置，直到保留解除為止。此程序必須是可稽核的。
*   **諮詢：** 必須諮詢法律和合規團隊，以定義和核准每個資料類別的特定保留期間，並確保政策符合所有法律義務。

## 6. 政策執行與職責

*   **所有權：** 指定的角色或團隊 (例如資料治理長、系統擁有者、合規團隊) 將負責此政策的整體所有權、維護和執行。
*   **自動化：** 政策執行 (封存、刪除) 應盡可能自動化，以確保一致性並減少人為錯誤。
*   **監控與稽核：** 定期稽核政策的實作情況，以確保其正確執行 (例如，驗證資料是否按排程封存/刪除)。
*   **訓練：** 相關人員應接受有關此政策及其職責的訓練。

## 7. 例外情況

*   此政策的任何例外情況都必須具有有效且記錄在案的業務或法律原因。
*   所有例外情況都必須由指定機構 (例如資料治理長、法務部門、高階管理層) 核准。
*   必須保留所有已核准例外情況的記錄。

此政策將至少每年檢閱一次，並在必要時進行更新，以反映業務需求、技術和法律/法規環境的變化。
