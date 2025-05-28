# Configuration Management Strategy

This document defines the strategy for managing application and infrastructure configurations across all environments for the CreditCard E-statement system.

## 1. Introduction & Goals

*   **Purpose:** To establish a consistent, secure, and auditable approach for managing all system configurations.
*   **Goals:**
    *   Ensure consistency of configurations across different deployment environments (development, testing, staging, production).
    *   Securely manage sensitive configuration data (secrets).
    *   Allow for easy modification and updates to configurations.
    *   Provide auditability for configuration changes.
    *   Support environment-specific settings without altering application code.
*   **Scope:** This strategy applies to configurations for all microservices, databases, message queues, API Gateway, AFP resources, third-party service integrations (e.g., email/SMS/push gateways), and operational parameters.

## 2. Configuration Storage & Formats

*   **Primary Configuration Formats:**
    *   **YAML (`.yaml` or `.yml`): Recommended for structured configuration.**
        *   **Rationale:** Supports hierarchical data structures, comments, and is generally more human-readable for complex configurations than JSON or `.env` files.
        *   Example: `config.base.yaml`, `config.production.yaml`.
    *   **`.env` Files (for environment variable definitions):**
        *   **Rationale:** Simple key-value pairs, easily loaded into environment variables by various tools and languages. Useful for bootstrapping or overriding specific settings.
        *   Example: `.env.production` (values from this would typically be loaded into the environment by the deployment system).
    *   **JSON (`.json`):** Can be used, but YAML is often preferred for manual editing. JSON is suitable for machine-generated or consumed configurations.
*   **Environment Variables as Primary Consumption Method:**
    *   Applications (especially those containerized with Docker) SHOULD primarily consume configuration values via environment variables.
    *   This decouples configuration from the application artifact (e.g., Docker image).
    *   Configuration values from YAML files, `.env` files, Kubernetes ConfigMaps/Secrets, or dedicated configuration services can be mapped to environment variables available to the application process.
*   **Version Control:**
    *   **Default/Base Configurations:** Non-sensitive default configuration files or templates (e.g., `config.defaults.yaml`, `.env.example`) SHOULD be committed to the Git repository alongside the application code. These define the structure and provide sensible defaults.
    *   **Environment-Specific Configurations:**
        *   Files containing secrets (e.g., database passwords, API keys) MUST NOT be committed to Git. Use `.gitignore` to explicitly exclude these files (e.g., `*.local.yaml`, `.env.*.local`, `secrets.yaml`).
        *   Non-sensitive environment-specific overrides can be version-controlled if appropriate, but secrets must always be externalized.

## 3. Environment-Specific Configurations

*   **Defined Environments:** The system will have clearly defined environments, such as:
    *   `development` (local developer machines)
    *   `ci` (continuous integration server)
    *   `testing` (shared QA environment)
    *   `staging` (pre-production, UAT environment)
    *   `production` (live environment)
*   **Loading Strategy & Precedence:**
    1.  **Application Defaults:** Hardcoded defaults within the application (should be minimal).
    2.  **Base Configuration File:** A base configuration file (e.g., `config.base.yaml`) loaded by the application, containing common settings.
    3.  **Environment-Specific Configuration File:** An environment-specific file (e.g., `config.production.yaml`, `config.development.yaml`) that overrides values from the base file. The selection of this file can be determined by an environment variable (e.g., `APP_ENV=production`).
    4.  **Environment Variables:** Values set as environment variables in the execution environment (e.g., set by Docker, Kubernetes, or the OS) take the highest precedence and override values from any loaded files.
    5.  **Externalized Secrets:** Secrets fetched from a dedicated secrets management system override any file-based or environment variable settings for those specific secret keys.
*   **Tools for Environment Management:**
    *   **Docker Compose (for local development):** Use `env_file` or `environment` directives in `docker-compose.yml` to manage environment-specific settings for local containers.
    *   **Kubernetes (for testing, staging, production):**
        *   **ConfigMaps:** Store non-sensitive configuration data.
        *   **Secrets:** Store sensitive configuration data (see next section).
        *   These are mounted into pods as environment variables or files.
        *   Helm charts can be used to template Kubernetes manifests and manage environment-specific values.

## 4. Sensitive Configuration (Secrets Management)

*   **Definition:** Any configuration parameter that, if exposed, could lead to a security compromise. Examples: API keys, database passwords, private cryptographic keys (for DKIM, TLS), `client_secret` for OAuth, encryption keys for data at rest.
*   **Storage and Access (Prioritized Options):**
    1.  **Dedicated Secrets Management System (Highly Recommended):**
        *   **Tools:** HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, Google Cloud Secret Manager.
        *   **Process:** Applications authenticate to the secrets manager at runtime (e.g., via IAM roles in cloud environments, Kubernetes service account integration) and fetch their required secrets.
        *   **Benefits:** Centralized control, strong encryption at rest and in transit, fine-grained access policies, audit trails for secret access, secret rotation capabilities.
    2.  **Kubernetes Secrets (with etcd encryption enabled):**
        *   **Usage:** Store secrets as native Kubernetes Secret objects.
        *   **Security Note:** By default, Kubernetes Secrets are only base64 encoded, not encrypted within etcd. It is CRUCIAL to enable encryption at rest for etcd in the Kubernetes cluster if using this as the primary mechanism for sensitive secrets.
        *   **Integration:** Can be integrated with external secrets managers (e.g., External Secrets Operator for K8s) to sync secrets from systems like Vault into native K8s Secrets, providing a hybrid approach.
    *   **Environment Variables (from secure injection):** Secrets can be injected as environment variables into containers by the orchestration platform (Kubernetes) from a secure source (like K8s Secrets or a secrets management tool). Application code should read them from the environment.
*   **Access Control:** Access to secrets (both in the management system and how they are exposed to applications) MUST be strictly controlled using the principle of least privilege.
*   **Rotation:** Implement policies and automated procedures for regularly rotating secrets (e.g., database passwords, API keys). Secrets management systems often provide features to facilitate this.

## 5. Configuration for Different Components

*   **Microservices (General):** Database connection strings, message queue broker addresses, API keys for third-party services (e.g., SMS/Push gateways), logging levels, retry policy parameters (max attempts, backoff intervals), feature flag settings, resource paths (if not discovered via service discovery).
*   **AFP Resources:** Paths to AFP font libraries, overlay directories, page segment directories. These will likely be configured as volume mounts in containers, with the mount paths potentially specified via environment variables derived from ConfigMaps.
*   **SMTP Settings (Email Sender Service):** SMTP server address, port, authentication credentials (username/password if using a relay), `MAIL FROM` address, DKIM selector and private key (the key itself is a secret).
*   **Conversion Tool Parameters (AFP Conversion Module):** Path to the external conversion tool executable or SDK configuration, license key/server information (if applicable), default conversion parameters (e.g., PDF/A compliance level).
*   **API Gateway:** Routing rules, rate limiting policies, timeout settings, security policy configurations (e.g., JWT validation parameters, OAuth provider details).
*   **Batch Orchestrator (Apache Airflow):** Database connection for its metadata store, executor configuration, connections to external services (e.g., Kubernetes cluster, cloud services), variables and macros for DAGs.

## 6. Configuration Management Tools & Practices

*   **Kubernetes ConfigMaps and Secrets:** As mentioned, these are the native way to manage application configuration in Kubernetes.
*   **Helm (for Kubernetes deployments):**
    *   Helm charts allow templating of Kubernetes manifests.
    *   Environment-specific configurations can be managed using different `values.yaml` files per environment or by overriding values during Helm deployment.
*   **Ansible, Chef, Puppet (less relevant for fully K8s-native apps):** If managing VMs or traditional server deployments, these tools can be used for configuration file management and distribution.
*   **Configuration Validation:**
    *   Applications SHOULD validate their configuration on startup.
    *   Fail fast if critical configurations are missing or invalid (e.g., unable to connect to database or message queue).
    *   Log any configuration issues clearly.
*   **Auditing Configuration Changes:**
    *   Changes to default configurations (in Git) are audited via Git history.
    *   Changes to live configurations (e.g., updating a ConfigMap in Kubernetes, modifying a secret in Vault) MUST be auditable. Secrets management systems and Kubernetes API server provide audit logs.
    *   Implement change management processes for production configuration changes.
*   **Dynamic Configuration / Feature Flags (Optional/Advanced):**
    *   **Concept:** For certain types of configuration (e.g., feature flags, tuning parameters, log levels), consider using a dynamic configuration system that allows changes to take effect without requiring an application restart.
    *   **Tools:** Spring Cloud Config Server, HashiCorp Consul, etcd, LaunchDarkly (for feature flags).
    *   **Considerations:** Adds complexity to the architecture. Use judiciously for parameters that genuinely need dynamic updates.

## 7. Review and Updates

*   Configuration parameters and the overall management strategy MUST be reviewed periodically (e.g., quarterly or as part of security reviews).
*   Remove obsolete or unused configuration parameters.
*   Update configurations as part of software upgrades (e.g., new parameters for a new version of a database or message queue).
*   Ensure documentation related to configuration is kept up-to-date.

This Configuration Management strategy aims to provide a secure, flexible, and auditable way to manage the diverse configuration needs of the CreditCard E-statement system.

---
## 正體中文 (Traditional Chinese)

# 組態管理策略

本文件定義了在信用卡電子帳單系統所有環境中管理應用程式和基礎架構組態的策略。

## 1. 簡介與目標

*   **目的：** 建立一致、安全且可稽核的方法來管理所有系統組態。
*   **目標：**
    *   確保不同部署環境 (開發、測試、預備、生產) 組態的一致性。
    *   安全地管理敏感組態資料 (機密)。
    *   允許輕鬆修改和更新組態。
    *   提供組態變更的可稽核性。
    *   支援環境特定設定，而無需更改應用程式程式碼。
*   **範圍：** 此策略適用於所有微服務、資料庫、訊息佇列、API 閘道、AFP 資源、第三方服務整合 (例如電子郵件/簡訊/推播閘道) 和營運參數的組態。

## 2. 組態儲存與格式

*   **主要組態格式：**
    *   **YAML (`.yaml` 或 `.yml`)：建議用於結構化組態。**
        *   **理由：** 支援階層式資料結構、註解，並且對於複雜組態通常比 JSON 或 `.env` 檔案更易於人工讀取。
        *   範例：`config.base.yaml`、`config.production.yaml`。
    *   **`.env` 檔案 (用於環境變數定義)：**
        *   **理由：** 簡單的鍵值對，易於由各種工具和語言載入到環境變數中。適用於啟動或覆寫特定設定。
        *   範例：`.env.production` (此檔案中的值通常由部署系統載入到環境中)。
    *   **JSON (`.json`)：** 可以使用，但對於手動編輯，通常首選 YAML。JSON 適用於機器產生或取用的組態。
*   **環境變數作為主要取用方法：**
    *   應用程式 (尤其是使用 Docker 容器化的應用程式) 應主要透過環境變數取用組態值。
    *   這將組態與應用程式成品 (例如 Docker 映像檔) 分離。
    *   來自 YAML 檔案、`.env` 檔案、Kubernetes ConfigMap/Secret 或專用組態服務的組態值可以對應到應用程式程序可用的環境變數。
*   **版本控制：**
    *   **預設/基礎組態：** 非敏感的預設組態檔或範本 (例如 `config.defaults.yaml`、`.env.example`) 應與應用程式程式碼一起提交到 Git 儲存庫。這些定義了結構並提供合理的預設值。
    *   **環境特定組態：**
        *   包含機密 (例如資料庫密碼、API 金鑰) 的檔案不得提交到 Git。使用 `.gitignore` 明確排除這些檔案 (例如 `*.local.yaml`、`.env.*.local`、`secrets.yaml`)。
        *   如果適當，可以對非敏感的環境特定覆寫進行版本控制，但機密必須始終外部化。

## 3. 環境特定組態

*   **已定義環境：** 系統將具有明確定義的環境，例如：
    *   `development` (本機開發人員機器)
    *   `ci` (持續整合伺服器)
    *   `testing` (共用 QA 環境)
    *   `staging` (預生產、UAT 環境)
    *   `production` (正式環境)
*   **載入策略與優先順序：**
    1.  **應用程式預設值：** 應用程式中硬式編碼的預設值 (應盡可能少)。
    2.  **基礎組態檔：** 由應用程式載入的基礎組態檔 (例如 `config.base.yaml`)，包含通用設定。
    3.  **環境特定組態檔：** 環境特定檔案 (例如 `config.production.yaml`、`config.development.yaml`)，用於覆寫基礎檔案中的值。此檔案的選擇可以由環境變數 (例如 `APP_ENV=production`) 決定。
    4.  **環境變數：** 在執行環境中設定為環境變數的值 (例如由 Docker、Kubernetes 或作業系統設定) 具有最高優先順序，並覆寫從任何載入檔案中取得的值。
    5.  **外部化機密：** 從專用機密管理系統擷取的機密會覆寫這些特定機密金鑰的任何基於檔案或環境變數的設定。
*   **環境管理工具：**
    *   **Docker Compose (用於本機開發)：** 在 `docker-compose.yml` 中使用 `env_file` 或 `environment` 指示詞來管理本機容器的環境特定設定。
    *   **Kubernetes (用於測試、預備、生產)：**
        *   **ConfigMap：** 儲存非敏感組態資料。
        *   **Secret：** 儲存敏感組態資料 (請參閱下一節)。
        *   這些會以環境變數或檔案的形式掛載到 pod 中。
        *   Helm 圖表可用於範本化 Kubernetes 清單並管理環境特定值。

## 4. 敏感組態 (機密管理)

*   **定義：** 任何如果洩露可能導致安全性漏洞的組態參數。範例：API 金鑰、資料庫密碼、私密加密金鑰 (用於 DKIM、TLS)、OAuth 的 `client_secret`、靜態資料的加密金鑰。
*   **儲存與存取 (優先選項)：**
    1.  **專用機密管理系統 (強烈建議)：**
        *   **工具：** HashiCorp Vault、AWS Secrets Manager、Azure Key Vault、Google Cloud Secret Manager。
        *   **程序：** 應用程式在執行階段向機密管理器進行驗證 (例如，透過雲端環境中的 IAM 角色、Kubernetes 服務帳戶整合) 並擷取其所需的機密。
        *   **優點：** 集中控制、靜態和傳輸中強力加密、細微性存取原則、機密存取稽核追蹤、機密輪換功能。
    2.  **Kubernetes Secret (啟用 etcd 加密)：**
        *   **用途：** 將機密儲存為原生 Kubernetes Secret 物件。
        *   **安全性注意事項：** 預設情況下，Kubernetes Secret 僅進行 base64 編碼，在 etcd 中未加密。如果將此作為敏感機密的主要機制，則在 Kubernetes 叢集中為 etcd 啟用靜態加密至關重要。
        *   **整合：** 可以與外部機密管理器 (例如 Kubernetes 的 External Secrets Operator) 整合，以將來自 Vault 等系統的機密同步到原生 K8s Secret，從而提供混合方法。
    *   **環境變數 (來自安全注入)：** 機密可以由協調平台 (Kubernetes) 從安全來源 (如 K8s Secret 或機密管理工具) 作為環境變數注入容器中。應用程式程式碼應從環境中讀取它們。
*   **存取控制：** 對機密的存取 (無論是在管理系統中還是在如何向應用程式公開) 都必須使用最小權限原則進行嚴格控制。
*   **輪換：** 實作定期輪換機密 (例如資料庫密碼、API 金鑰) 的原則和自動化程序。機密管理系統通常提供有助於此的功能。

## 5. 不同元件的組態

*   **微服務 (一般)：** 資料庫連線字串、訊息佇列代理程式位址、第三方服務的 API 金鑰 (例如簡訊/推播閘道)、日誌記錄層級、重試原則參數 (最大嘗試次數、退避間隔)、功能旗標設定、資源路徑 (如果未透過服務探索發現)。
*   **AFP 資源：** AFP 字型庫、疊加目錄、頁面區段目錄的路徑。這些很可能設定為容器中的磁碟區掛載，掛載路徑可能透過衍生自 ConfigMap 的環境變數指定。
*   **SMTP 設定 (電子郵件傳送服務)：** SMTP 伺服器位址、連接埠、驗證憑證 (如果使用中繼，則為使用者名稱/密碼)、`MAIL FROM` 位址、DKIM 選擇器和私密金鑰 (金鑰本身是機密)。
*   **轉換工具參數 (AFP 轉換模組)：** 外部轉換工具可執行檔或 SDK 組態的路徑、授權金鑰/伺服器資訊 (如果適用)、預設轉換參數 (例如 PDF/A 相容性層級)。
*   **API 閘道：** 路由規則、速率限制原則、逾時設定、安全性原則組態 (例如 JWT 驗證參數、OAuth 提供者詳細資訊)。
*   **批次協調器 (Apache Airflow)：** 其元資料儲存的資料庫連線、執行器組態、與外部服務的連線 (例如 Kubernetes 叢集、雲端服務)、DAG 的變數和巨集。

## 6. 組態管理工具與實務

*   **Kubernetes ConfigMap 與 Secret：** 如前所述，這些是在 Kubernetes 中管理應用程式組態的原生方法。
*   **Helm (用於 Kubernetes 部署)：**
    *   Helm 圖表允許範本化 Kubernetes 清單。
    *   可以使用每個環境不同的 `values.yaml` 檔案或在 Helm 部署期間覆寫值來管理環境特定組態。
*   **Ansible、Chef、Puppet (對於完全 K8s 原生應用程式較不相關)：** 如果管理 VM 或傳統伺服器部署，這些工具可用於組態檔管理和分發。
*   **組態驗證：**
    *   應用程式應在啟動時驗證其組態。
    *   如果關鍵組態遺失或無效 (例如，無法連線到資料庫或訊息佇列)，則快速失敗。
    *   清楚記錄任何組態問題。
*   **稽核組態變更：**
    *   對預設組態 (在 Git 中) 的變更透過 Git 歷史記錄進行稽核。
    *   對即時組態 (例如，更新 Kubernetes 中的 ConfigMap、修改 Vault 中的機密) 的變更必須是可稽核的。機密管理系統和 Kubernetes API 伺服器提供稽核記錄。
    *   實作生產組態變更的變更管理程序。
*   **動態組態 / 功能旗標 (選用/進階)：**
    *   **概念：** 對於某些類型的組態 (例如功能旗標、調整參數、日誌記錄層級)，請考慮使用動態組態系統，允許變更生效而無需重新啟動應用程式。
    *   **工具：** Spring Cloud Config Server、HashiCorp Consul、etcd、LaunchDarkly (用於功能旗標)。
    *   **考量：** 增加架構的複雜性。謹慎使用於確實需要動態更新的參數。

## 7. 檢閱與更新

*   組態參數和整體管理策略必須定期檢閱 (例如，每季或作為安全性檢閱的一部分)。
*   移除過時或未使用的組態參數。
*   作為軟體升級的一部分更新組態 (例如，新版資料庫或訊息佇列的新參數)。
*   確保與組態相關的文件保持最新。

此組態管理策略旨在為管理信用卡電子帳單系統的多樣化組態需求提供安全、彈性且可稽核的方法。
