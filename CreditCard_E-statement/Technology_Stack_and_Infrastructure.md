# Technology Stack and Infrastructure Considerations

This document outlines the proposed technology stack and infrastructure considerations for the comprehensive AFP document generation, conversion, delivery, and management system.

## 1. Consolidated Technology Stack

This section details the recommended technologies for building the various microservices and components of the platform.

*   **Primary Programming Languages & Frameworks:**
    *   **Java with Spring Boot (Primary for most backend services):**
        *   **Justification:** Robust, mature ecosystem with extensive libraries for enterprise applications (e.g., Spring Data, Spring Security, Spring AMQP for RabbitMQ). Strong performance, good for building scalable microservices. Large talent pool available. Chosen for services like Core AFP Generation Engine, AFP Conversion Module wrapper, Email Sender Service, SMSSenderService, AppPushService, CustomerProfileService, and potentially a dedicated Orchestration Service if not using Airflow for all orchestration.
    *   **Python with Apache Airflow (for Workflow Orchestration):**
        *   **Justification:** Airflow is Python-native, making it the natural choice for defining and managing complex batch workflows (DAGs). Python's scripting capabilities are also excellent for operational tasks and smaller utility services.
    *   **.NET Core (Alternative for backend services):**
        *   **Justification:** A viable alternative to Java/Spring Boot, especially if the development team has strong C#/.NET expertise. Offers high performance, a modern framework, and excellent cross-platform support. Libraries like MailKit for email are robust.
    *   **JavaScript/TypeScript with a modern frontend framework (e.g., React, Angular, Vue.js - for Visual Template Builder UI):**
        *   **Justification:** Standard for building interactive web-based user interfaces. The choice of specific framework can depend on team preference and existing skillsets.

*   **Databases:**
    *   **PostgreSQL (Primary Relational Database):**
        *   **Justification:** Powerful open-source RDBMS with strong support for transactions, JSONB (for flexible data like template structures or preferences), and scalability. Good for structured data like job statuses, customer profiles (if relational model is chosen), and application metadata.
    *   **MongoDB (Alternative/Supplementary NoSQL Database):**
        *   **Justification:** Suitable for storing unstructured or semi-structured data, such as customer preferences, template definitions (especially if highly dynamic or complex JSON), or logs. Offers horizontal scalability and flexibility. Could be used for the `CustomerProfileService` or `TemplateManagementService` if a document-oriented model is preferred.
    *   **Prometheus (Time-Series Database for Metrics):**
        *   **Justification:** Industry standard for storing time-series metrics data collected from applications and infrastructure. Integrates seamlessly with Grafana for visualization.

*   **Message Queues:**
    *   **RabbitMQ (Primary Choice):**
        *   **Justification:** Mature, feature-rich (persistent messages, dead-letter exchanges, flexible routing options, message acknowledgments, priority queues), and widely adopted with good client library support across Java, Python, and .NET. Ideal for decoupling microservices and managing asynchronous tasks like email sending, AFP generation requests, and conversion jobs.
    *   **Apache Kafka (Alternative for high-throughput/event streaming):**
        *   **Justification:** Suitable if the system evolves to handle extremely high volumes of events or requires a durable, distributed log for event sourcing. More complex to manage than RabbitMQ and might be overkill for initial requirements but a good option for future scalability in event-driven scenarios.

*   **AFP Generation Specific Tools:**
    *   **Apache FOP (Formatting Objects Processor):**
        *   **Justification:** Open-source Java library that supports AFP output from XSL-FO input. Suitable for the Core AFP Generation Engine.
    *   **Barcode Libraries (e.g., Barcode4J for Java, python-barcode for Python):**
        *   **Justification:** To generate barcodes (Code 128, QR codes, etc.) that can be embedded as images within the AFP documents via FOP.
    *   **Charting Libraries (e.g., JFreeChart for Java, Matplotlib/Seaborn for Python if pre-generating images):**
        *   **Justification:** For generating charts as images that can be embedded into AFP documents. FOP itself has limited native charting capabilities.

*   **AFP Conversion Specific Tools:**
    *   **Commercial Conversion Engines (Primary approach):**
        *   **Justification:** As detailed in `AFP_Conversion_Module_Design.md`, high-fidelity conversion of complex AFP files (especially those using MO:DCA IS/3 features like FS45 color, complex overlays, and diverse font technologies) generally requires specialized commercial tools. Examples include solutions from Crawford Technologies, OpenText, Precisely, or Compart. The `AFPConversionService` will wrap such a tool.
    *   **Open Source (Limited, for basic needs or as fallback if specific features are not critical):**
        *   Ghostscript (with potential AFP input plugins, though often limited).
        *   Apache FOP (can render some AFP, but not a general-purpose converter).
        *   **Justification:** Lower cost, but likely significant limitations in feature support and fidelity for enterprise-grade AFP documents.

*   **API Gateway:**
    *   **Options:** Kong, Tyk (Open Source with commercial support), Spring Cloud Gateway (if Java/Spring ecosystem is dominant), AWS API Gateway, Azure API Management, Google Cloud API Gateway.
    *   **Justification:** Provides a single entry point for external APIs, handling routing, authentication/authorization, rate limiting, logging, and SSL termination, as detailed in `RESTful_APIs_External_Integration_Design.md`. The specific choice depends on infrastructure (cloud vs. on-premise) and existing investments.

*   **Workflow Orchestration:**
    *   **Apache Airflow (Primary Choice):**
        *   **Justification:** Python-based, highly extensible, excellent for defining, scheduling, and monitoring complex batch workflows (DAGs) that involve multiple microservices. Good UI for operational management.
    *   **Alternative (Java-based):** A custom orchestration service using Spring Batch for job processing logic and a scheduler like Quartz, or a more lightweight workflow engine if Airflow is deemed too heavy.

*   **Caching:**
    *   **Redis (Primary Choice):**
        *   **Justification:** High-performance, in-memory data store, versatile (can be used for caching, session management, rate limiting counters, short-term job metadata). Widely supported.
    *   **Memcached (Alternative):**
        *   **Justification:** Simpler, pure in-memory key-value store, good for caching. Less feature-rich than Redis.
    *   **Use Cases:** Caching frequently accessed customer preferences, template structures, DNS MX records, API Gateway responses.

## 2. Infrastructure Considerations

*   **Deployment Strategy:**
    *   **Preferred Approach: Cloud-Native (AWS, Azure, or GCP)**
        *   **Pros:** Scalability on demand, managed services (databases, message queues, Kubernetes, API Gateways), pay-as-you-go, global reach, rich set of supporting services for logging, monitoring, security.
        *   **Cons:** Potential vendor lock-in, data residency/sovereignty concerns (must be addressed with region selection and data handling policies), potentially higher operational costs if not managed carefully.
    *   **On-Premise:**
        *   **Pros:** Full control over hardware and data, potentially lower long-term costs for stable workloads if existing infrastructure is leveraged, easier to meet strict data residency requirements.
        *   **Cons:** Higher upfront investment, responsibility for all infrastructure management (hardware, OS, networking, scaling, HA), slower to scale.
    *   **Hybrid:**
        *   **Pros:** Combines benefits, e.g., sensitive data/core processing on-premise, with scalable front-ends or specific services (like burstable conversion workers) in the cloud.
        *   **Cons:** Increased complexity in managing and integrating two different environments.
    *   **Recommendation:** Cloud-Native is generally preferred for new systems due to flexibility and scalability, but specific business constraints (security, data sovereignty, existing infrastructure) might dictate On-Premise or Hybrid.

*   **Containerization & Orchestration:**
    *   **Docker (for Containerization):**
        *   **Justification:** Standardizes the deployment unit for all microservices, ensuring consistency across environments (dev, test, prod). Simplifies dependency management.
    *   **Kubernetes (K8s - for Orchestration):**
        *   **Justification:** De facto standard for managing containerized applications at scale. Provides automated deployment, scaling, self-healing, service discovery, and load balancing. Managed Kubernetes services (EKS, AKS, GKE) reduce operational overhead.

*   **Scalability:**
    *   **Microservices:** Each microservice (AFP Generation, Conversion, Email Sender, etc.) can be scaled horizontally by running multiple instances, orchestrated by Kubernetes.
    *   **Databases:**
        *   PostgreSQL: Can be scaled using read replicas, connection pooling, and eventually sharding if write loads become extreme. Managed cloud database services offer easier scaling.
        *   MongoDB: Designed for horizontal scaling via sharding.
        *   Prometheus: Can be scaled with federation or solutions like Thanos/Cortex for long-term storage and global view.
    *   **Message Queues:**
        *   RabbitMQ: Can be clustered for high availability and load distribution.
        *   Kafka: Natively designed for horizontal scaling.
    *   **API Gateway:** Most API Gateway solutions are designed to be scalable.
    *   **Airflow:** Workers can be scaled out. CeleryExecutor with RabbitMQ/Redis or KubernetesExecutor provides scalability.

*   **High Availability (HA) & Disaster Recovery (DR):**
    *   **Redundancy:** Deploy multiple instances of each microservice, API Gateway, and Airflow component across different availability zones (in cloud) or physical servers (on-premise).
    *   **Load Balancing:** Use load balancers (cloud provider's LB, Nginx, HAProxy) in front of stateless services.
    *   **Database HA:** Use managed database services with HA configurations (e.g., automated failover to replicas) or set up primary-standby replication for self-managed databases.
    *   **Message Queue HA:** RabbitMQ clustering with mirrored queues. Kafka's distributed nature provides HA.
    *   **Backup & DR:**
        *   Regular automated backups for all persistent datastores (PostgreSQL, MongoDB, Prometheus data if needed long-term).
        *   Test data restoration procedures.
        *   For DR, consider multi-region deployments for critical components or at least have a plan to redeploy and restore data in a different region/datacenter.

*   **Security Infrastructure Aspects (Brief Overview):**
    *   **Network Segmentation:** Use Virtual Private Clouds (VPCs), subnets, and firewalls/security groups to isolate different parts of the system (e.g., public-facing API Gateway, internal services, databases).
    *   **Secrets Management:** Use a dedicated secrets management solution (e.g., HashiCorp Vault, AWS Secrets Manager, Azure Key Vault) for storing API keys, database credentials, private keys (for DKIM), etc. Do not hardcode secrets.
    *   **Secure Communication:**
        *   TLS for all external API communication (terminated at API Gateway).
        *   TLS for communication between API Gateway and internal microservices.
        *   TLS for internal service-to-service communication where sensitive data is exchanged.
        *   Secure connections to databases and message queues.
    *   (A full security design will be detailed in a subsequent document).

*   **Logging and Monitoring Infrastructure:**
    *   **Centralized Logging:**
        *   **ELK Stack (Elasticsearch, Logstash, Kibana):** Powerful and widely used for collecting, parsing, storing, and visualizing logs from all services and infrastructure components.
        *   **Grafana Loki:** A lighter-weight alternative, often paired with Prometheus for metrics, good for log aggregation and querying.
        *   Services should output logs in a structured format (e.g., JSON) to facilitate parsing.
    *   **Metrics Collection:**
        *   **Prometheus:** As mentioned, for collecting time-series metrics.
        *   Services expose metrics via a `/metrics` endpoint (standard for Prometheus).
    *   **Dashboarding & Visualization:**
        *   **Grafana:** For creating dashboards from Prometheus metrics and visualizing logs from Elasticsearch/Loki.
    *   **Alerting:**
        *   **Alertmanager (Prometheus ecosystem):** For defining alerts based on Prometheus metrics, handling deduplication, grouping, and routing alerts to various notification channels (email, Slack, PagerDuty).
        *   ElastAlert for alerting based on log patterns in Elasticsearch.

This document provides a high-level overview of the technology stack and infrastructure. Specific versions, configurations, and detailed deployment architectures for each component will be refined during the implementation planning phase.The design document `Technology_Stack_and_Infrastructure.md` has been created. It covers:

1.  **Consolidated Technology Stack:**
    *   Primary languages/frameworks (Java/Spring Boot, Python/Airflow, .NET Core alternative, JavaScript/TypeScript for UI).
    *   Databases (PostgreSQL, MongoDB alternative, Prometheus for metrics).
    *   Message Queues (RabbitMQ, Kafka alternative).
    *   AFP Generation tools (Apache FOP, barcode/charting libraries).
    *   AFP Conversion tools (reiterating reliance on commercial tools).
    *   API Gateway options (Kong, Spring Cloud Gateway, cloud-specific).
    *   Workflow Orchestration (Apache Airflow).
    *   Caching (Redis, Memcached).
2.  **Infrastructure Considerations:**
    *   Deployment Strategy (Cloud-Native preferred, On-Premise/Hybrid options).
    *   Containerization & Orchestration (Docker & Kubernetes).
    *   Scalability strategies for services, databases, and queues.
    *   High Availability measures (redundancy, load balancing, HA for stateful components).
    *   Security Infrastructure aspects (network segmentation, secrets management, secure communication).
    *   Logging and Monitoring Infrastructure (ELK/Loki, Prometheus, Grafana, Alertmanager).

This document synthesizes previous discussions and provides a coherent overview for these topics.

---
## 正體中文 (Traditional Chinese)

# 技術堆疊與基礎架構考量

本文件概述了綜合性 AFP 文件產生、轉換、交付和管理系統的建議技術堆疊與基礎架構考量。

## 1. 整合技術堆疊

本節詳細說明了建置平台各種微服務和元件的建議技術。

*   **主要程式語言與框架：**
    *   **Java 搭配 Spring Boot (多數後端服務的主要選擇)：**
        *   **理由：** 健全、成熟的生態系統，擁有適用於企業應用程式的廣泛函式庫 (例如 Spring Data、Spring Security、用於 RabbitMQ 的 Spring AMQP)。效能強大，適合建置可擴展的微服務。擁有大量可用人才。已選擇用於核心 AFP 產生引擎、AFP 轉換模組包裝器、電子郵件傳送服務、簡訊傳送服務、應用程式推播服務、客戶設定檔服務等服務，以及如果未使用 Airflow 進行所有協調，則可能用於專用的協調服務。
    *   **Python 搭配 Apache Airflow (用於工作流程協調)：**
        *   **理由：** Airflow 是 Python 原生的，使其成為定義和管理複雜批次工作流程 (DAG) 的自然選擇。Python 的腳本編寫能力也非常適合營運任務和小型公用程式服務。
    *   **.NET Core (後端服務的替代方案)：**
        *   **理由：** Java/Spring Boot 的可行替代方案，尤其是在開發團隊擁有強大的 C#/.NET 專業知識的情況下。提供高效能、現代化的框架和出色的跨平台支援。像 MailKit 這樣的電子郵件函式庫功能強大。
    *   **JavaScript/TypeScript 搭配現代前端框架 (例如 React、Angular、Vue.js - 用於視覺化範本產生器 UI)：**
        *   **理由：** 建置互動式 Web 使用者介面的標準。特定框架的選擇取決於團隊偏好和現有技能組合。

*   **資料庫：**
    *   **PostgreSQL (主要關聯式資料庫)：**
        *   **理由：** 功能強大的開源 RDBMS，強力支援交易、JSONB (用於像範本結構或偏好設定這樣的彈性資料) 和可擴展性。適用於結構化資料，如工作狀態、客戶設定檔 (如果選擇關聯式模型) 和應用程式中繼資料。
    *   **MongoDB (替代/補充 NoSQL 資料庫)：**
        *   **理由：** 適用於儲存非結構化或半結構化資料，例如客戶偏好設定、範本定義 (尤其是高度動態或複雜的 JSON) 或日誌。提供水平可擴展性和彈性。如果偏好文件導向模型，可用於 `CustomerProfileService` 或 `TemplateManagementService`。
    *   **Prometheus (用於指標的時間序列資料庫)：**
        *   **理由：** 儲存從應用程式和基礎架構收集的時間序列指標資料的業界標準。與 Grafana 無縫整合以進行視覺化。

*   **訊息佇列：**
    *   **RabbitMQ (主要選擇)：**
        *   **理由：** 成熟、功能豐富 (永續性訊息、無效信件交換、彈性路由選項、訊息確認、優先順序佇列)，並在 Java、Python 和 .NET 中廣泛採用並提供良好的用戶端函式庫支援。非常適合解耦微服務和管理非同步工作，如電子郵件傳送、AFP 產生請求和轉換工作。
    *   **Apache Kafka (高輸送量/事件串流的替代方案)：**
        *   **理由：** 如果系統發展到需要處理極高容量的事件或需要用於事件溯源的耐用分散式日誌，則適用。管理比 RabbitMQ 複雜，對於初始需求可能過於龐大，但在事件驅動情境中是未來可擴展性的不錯選擇。

*   **AFP 產生特定工具：**
    *   **Apache FOP (格式化物件處理器)：**
        *   **理由：** 開源 Java 函式庫，支援從 XSL-FO 輸入產生 AFP 輸出。適用於核心 AFP 產生引擎。
    *   **條碼函式庫 (例如 Java 的 Barcode4J、Python 的 python-barcode)：**
        *   **理由：** 產生可透過 FOP 嵌入 AFP 文件中的條碼 (Code 128、QR 碼等) 作為影像。
    *   **圖表函式庫 (例如 Java 的 JFreeChart、Python 的 Matplotlib/Seaborn (如果預先產生影像))：**
        *   **理由：** 將圖表產生為可嵌入 AFP 文件的影像。FOP 本身的本機圖表功能有限。

*   **AFP 轉換特定工具：**
    *   **商業轉換引擎 (主要方法)：**
        *   **理由：** 如 `AFP_Conversion_Module_Design.md` 中所述，高保真度轉換複雜的 AFP 檔案 (尤其是使用 MO:DCA IS/3 功能，如 FS45 色彩、複雜疊加層和多種字型技術的檔案) 通常需要專門的商業工具。範例包括 Crawford Technologies、OpenText、Precisely 或 Compart 的解決方案。`AFPConversionService` 將包裝此類工具。
    *   **開源 (有限，適用於基本需求或在特定功能不重要的情況下作為後備)：**
        *   Ghostscript (可能帶有 AFP 輸入外掛程式，但通常功能有限)。
        *   Apache FOP (可以呈現某些 AFP，但不是通用的轉換器)。
        *   **理由：** 成本較低，但對於企業級 AFP 文件，在功能支援和保真度方面可能存在重大限制。

*   **API 閘道：**
    *   **選項：** Kong、Tyk (開源並提供商業支援)、Spring Cloud Gateway (如果 Java/Spring 生態系統佔主導地位)、AWS API Gateway、Azure API Management、Google Cloud API Gateway。
    *   **理由：** 為外部 API 提供單一進入點，處理路由、驗證/授權、速率限制、記錄和 SSL 終止，如 `RESTful_APIs_External_Integration_Design.md` 中所述。具體選擇取決於基礎架構 (雲端與本地部署) 和現有投資。

*   **工作流程協調：**
    *   **Apache Airflow (主要選擇)：**
        *   **理由：** 基於 Python，高度可擴展，非常適合定義、排程和監控涉及多個微服務的複雜批次工作流程 (DAGs)。提供良好的營運管理 UI。
    *   **替代方案 (基於 Java)：** 使用 Spring Batch 進行工作處理邏輯和像 Quartz 這樣的排程器的自訂協調服務，或者如果認為 Airflow 過於笨重，則使用更輕量級的工作流程引擎。

*   **快取：**
    *   **Redis (主要選擇)：**
        *   **理由：** 高效能的記憶體內資料儲存，功能多樣 (可用於快取、工作階段管理、速率限制計數器、短期工作元資料)。廣泛支援。
    *   **Memcached (替代方案)：**
        *   **理由：** 更簡單、純粹的記憶體內鍵值儲存，適用於快取。功能不如 Redis 豐富。
    *   **使用案例：** 快取經常存取的客戶偏好設定、範本結構、DNS MX 記錄、API 閘道回應。

## 2. 基礎架構考量

*   **部署策略：**
    *   **建議方法：雲端原生 (AWS、Azure 或 GCP)**
        *   **優點：** 隨需擴展、受管理服務 (資料庫、訊息佇列、Kubernetes、API 閘道)、按使用付費、全球覆蓋、豐富的記錄、監控、安全性支援服務。
        *   **缺點：** 可能的供應商鎖定、資料落地/主權問題 (必須透過區域選擇和資料處理原則解決)、如果管理不善，營運成本可能更高。
    *   **本地部署：**
        *   **優點：** 完全控制硬體和資料，如果利用現有基礎架構，對於穩定工作負載而言長期成本可能較低，更容易滿足嚴格的資料落地要求。
        *   **缺點：** 前期投資較高，負責所有基礎架構管理 (硬體、作業系統、網路、擴展、高可用性)，擴展速度較慢。
    *   **混合雲：**
        *   **優點：** 結合兩者優點，例如將敏感資料/核心處理置於本地部署，同時在雲端部署可擴展的前端或特定服務 (如可彈性擴展的轉換 worker)。
        *   **缺點：** 管理和整合兩個不同環境的複雜性增加。
    *   **建議：** 由於彈性和可擴展性，新系統通常建議採用雲端原生，但特定的業務限制 (安全性、資料主權、現有基礎架構) 可能會決定採用本地部署或混合雲。

*   **容器化與協同運作：**
    *   **Docker (用於容器化)：**
        *   **理由：** 標準化所有微服務的部署單元，確保跨環境 (開發、測試、生產) 的一致性。簡化相依性管理。
    *   **Kubernetes (K8s - 用於協同運作)：**
        *   **理由：** 大規模管理容器化應用程式的事實標準。提供自動化部署、擴展、自我修復、服務探索和負載平衡。受管理的 Kubernetes 服務 (EKS、AKS、GKE) 可減少營運開銷。

*   **可擴展性：**
    *   **微服務：** 每個微服務 (AFP 產生、轉換、電子郵件傳送器等) 都可以透過執行多個執行個體進行水平擴展，並由 Kubernetes 協同運作。
    *   **資料庫：**
        *   PostgreSQL：可以使用讀取複本、連線池以及最終在寫入負載極高時使用分片進行擴展。受管理的雲端資料庫服務提供更輕鬆的擴展。
        *   MongoDB：專為透過分片進行水平擴展而設計。
        *   Prometheus：可以透過聯合或像 Thanos/Cortex 這樣的解決方案進行擴展，以實現長期儲存和全域檢視。
    *   **訊息佇列：**
        *   RabbitMQ：可以叢集化以實現高可用性和負載分配。
        *   Kafka：原生設計用於水平擴展。
    *   **API 閘道：** 大多數 API 閘道解決方案都設計為可擴展的。
    *   **Airflow：** Worker 可以擴展。CeleryExecutor 搭配 RabbitMQ/Redis 或 KubernetesExecutor 提供可擴展性。

*   **高可用性 (HA) 與災難復原 (DR)：**
    *   **備援：** 在不同可用區域 (雲端中) 或實體伺服器 (本地部署) 上部署每個微服務、API 閘道和 Airflow 元件的多個執行個體。
    *   **負載平衡：** 在無狀態服務前使用負載平衡器 (雲端供應商的 LB、Nginx、HAProxy)。
    *   **資料庫高可用性：** 使用具有高可用性組態的受管理資料庫服務 (例如自動容錯移轉到複本) 或為自行管理的資料庫設定主從式複製。
    *   **訊息佇列高可用性：** RabbitMQ 叢集搭配鏡像佇列。Kafka 的分散式特性提供高可用性。
    *   **備份與災難復原：**
        *   對所有永續性資料儲存庫 (PostgreSQL、MongoDB、Prometheus 資料 (如果需要長期儲存)) 進行定期自動備份。
        *   測試資料還原程序。
        *   對於災難復原，請考慮對關鍵元件進行多區域部署，或至少制定在不同區域/資料中心重新部署和還原資料的計畫。

*   **安全性基礎架構方面 (簡要概述)：**
    *   **網路區隔：** 使用虛擬私有雲 (VPC)、子網路和防火牆/安全性群組隔離系統的不同部分 (例如公開的 API 閘道、內部服務、資料庫)。
    *   **機密管理：** 使用專用的機密管理解決方案 (例如 HashiCorp Vault、AWS Secrets Manager、Azure Key Vault) 儲存 API 金鑰、資料庫憑證、私密金鑰 (用於 DKIM) 等。請勿將機密硬式編碼。
    *   **安全通訊：**
        *   所有外部 API 通訊使用 TLS (在 API 閘道終止)。
        *   API 閘道與內部微服務之間的通訊使用 TLS。
        *   交換敏感資料的內部服務對服務通訊使用 TLS。
        *   與資料庫和訊息佇列的安全連線。
    *   (完整的安全性設計將在後續文件中詳細說明)。

*   **記錄與監控基礎架構：**
    *   **集中式日誌記錄：**
        *   **ELK Stack (Elasticsearch、Logstash、Kibana)：** 功能強大且廣泛使用，用於收集、剖析、儲存和視覺化來自所有服務和基礎架構元件的日誌。
        *   **Grafana Loki：** 一個較輕量級的替代方案，通常與 Prometheus 搭配使用於指標監控，適用於日誌聚合和查詢。
        *   服務應以結構化格式 (例如 JSON) 輸出日誌，以利於剖析。
    *   **指標收集：**
        *   **Prometheus：** 如前所述，用於收集時間序列指標。
        *   服務透過 `/metrics` 端點公開指標 (Prometheus 的標準)。
    *   **儀表板與視覺化：**
        *   **Grafana：** 用於從 Prometheus 指標建立儀表板並視覺化來自 Elasticsearch/Loki 的日誌。
    *   **警示：**
        *   **Alertmanager (Prometheus 生態系統)：** 用於根據 Prometheus 指標定義警示、處理重複資料刪除、分組並將警示路由到各種通知通道 (電子郵件、Slack、PagerDuty)。
        *   ElastAlert 用於根據 Elasticsearch 中的日誌模式發出警示。

本文件提供了技術堆疊和基礎架構的高階概觀。每個元件的具體版本、組態和詳細部署架構將在實作規劃階段進行完善。
