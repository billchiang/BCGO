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
