# Deployment and Maintenance Plan

This document outlines the deployment and ongoing maintenance strategies for the AFP document generation, conversion, delivery, and management system. The aim is to establish a clear operational framework for deploying, managing, and updating the system reliably and efficiently.

## 1. Deployment Strategy & Process

A modern, automated approach to deployment will be adopted, leveraging containerization, orchestration, CI/CD, and Infrastructure as Code.

*   **Containerization (Docker):**
    *   All microservices (Core AFP Generation Engine, AFP Conversion Module, Email Sender Service, SMSSenderService, AppPushService, CustomerProfileService, DispatchService, API Gateway components if self-hosted, etc.) will be packaged as Docker containers.
    *   **Benefits:** Consistent environments across development, testing, and production; simplified dependency management; isolation of services; faster deployments.
    *   Dockerfiles will be maintained for each service to define the build process for its container image.
    *   Container images will be stored in a private container registry (e.g., AWS ECR, Azure ACR, Google GCR, Harbor).

*   **Orchestration (Kubernetes - K8s):**
    *   Kubernetes will be used to orchestrate the deployment, scaling, and management of containerized applications.
    *   **Benefits:** Automated rollouts and rollbacks, service discovery, load balancing, self-healing (restarting failed containers), horizontal scaling, configuration management.
    *   Kubernetes manifests (YAML files defining Deployments, Services, ConfigMaps, Secrets, Ingress, etc.) will be created for each microservice.
    *   Managed Kubernetes services (EKS, AKS, GKE) are recommended for cloud deployments to reduce operational overhead.

*   **CI/CD Pipeline:**
    *   A robust Continuous Integration/Continuous Delivery (CI/CD) pipeline is essential for automated and reliable deployments.
    *   **Tools:** Jenkins, GitLab CI/CD, GitHub Actions, Azure DevOps Pipelines.
    *   **Pipeline Stages:**
        1.  **Code Commit:** Developer commits code to a Git repository (e.g., GitLab, GitHub, Bitbucket).
        2.  **Build & Unit Test:**
            *   CI server triggers a build.
            *   Source code is compiled.
            *   Automated unit tests are executed. If tests fail, the build fails.
        3.  **Static Code Analysis & Security Scan (SAST/SCA):**
            *   Code is analyzed for quality issues and known vulnerabilities (e.g., SonarQube, OWASP Dependency-Check).
        4.  **Containerize:**
            *   If build and tests pass, a Docker image is built and tagged (e.g., with Git commit hash and/or version number).
            *   Image is pushed to the private container registry.
        5.  **Deploy to Dev/Test Environment:**
            *   Automated deployment of the new image to a dedicated development or testing Kubernetes namespace/cluster.
        6.  **Automated Integration & E2E Tests:**
            *   Automated integration tests and critical end-to-end tests are run against the deployed application in the dev/test environment.
        7.  **Deploy to Staging Environment:**
            *   If all previous stages pass, automated or manual promotion to the Staging environment (a production replica).
        8.  **User Acceptance Testing (UAT):**
            *   Stakeholders perform UAT in the Staging environment.
            *   Performance and more extensive security tests (e.g., DAST) may also be run here.
        9.  **Approval for Production Deployment:**
            *   Manual approval gate based on successful UAT and test results.
        10. **Deploy to Production:**
            *   Deployment to the production Kubernetes cluster using a chosen strategy (see below).
        11. **Post-Deployment Monitoring & Smoke Tests:**
            *   Monitor key metrics and logs immediately after deployment.
            *   Run automated smoke tests to verify critical functionality in production.

*   **Production Deployment Strategies:**
    *   **Rolling Update (Default Kubernetes Strategy):**
        *   Gradually replaces old versions of application instances with new ones, one by one or in batches.
        *   Ensures zero downtime if new version is stable.
        *   Can be configured with readiness and liveness probes to ensure new instances are healthy before traffic is shifted.
        *   **Recommendation:** Suitable for most microservice updates.
    *   **Blue/Green Deployment:**
        *   Two identical production environments ("Blue" and "Green") are maintained. Only one environment is live at any time.
        *   Deploy the new version to the idle environment (e.g., Green).
        *   After testing the Green environment, switch traffic from Blue to Green.
        *   **Pros:** Instant rollback by switching traffic back to Blue if issues occur.
        *   **Cons:** Requires double the infrastructure resources.
        *   **Recommendation:** Consider for critical services or major updates where rollback risk is high.
    *   **Canary Deployment:**
        *   Release the new version to a small subset of users/traffic.
        *   Monitor performance and errors closely.
        *   If stable, gradually roll out the new version to the rest of the users/traffic.
        *   **Pros:** Limits the impact of a faulty release. Allows for real-world testing with a small blast radius.
        *   **Cons:** More complex to set up and manage traffic routing (often requires service mesh like Istio or Linkerd, or advanced Ingress controller features).
        *   **Recommendation:** Advanced strategy, useful for high-risk changes or when A/B testing features.

*   **Infrastructure as Code (IaC):**
    *   Manage and provision infrastructure (VPCs, subnets, Kubernetes clusters, databases, message queues, load balancers) using code.
    *   **Tools:** Terraform (cloud-agnostic), AWS CloudFormation, Azure Resource Manager (ARM) templates, Google Cloud Deployment Manager.
    *   **Benefits:** Repeatable and consistent environments, version control for infrastructure, automated provisioning, reduced manual errors.

*   **Configuration Management:**
    *   **Kubernetes ConfigMaps:** Store non-sensitive configuration data (e.g., application settings, resource paths, default values) as ConfigMaps in Kubernetes. These can be mounted as files or environment variables into containers.
    *   **Kubernetes Secrets:** Store sensitive configuration data (API keys, database passwords, TLS certificates, DKIM private keys) as Kubernetes Secrets. These are base64 encoded (not truly encrypted at rest by default in etcd, so ensure etcd encryption is enabled or use an external secrets manager).
    *   **External Secrets Management (Recommended for higher security):** Integrate Kubernetes with systems like HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault to securely inject secrets into pods at runtime. This avoids storing sensitive data directly in Kubernetes etcd.

## 2. Ongoing Maintenance

Regular maintenance is crucial for security, stability, and performance.

*   **Regular Patching and Updates:**
    *   **Operating Systems (OS):** Apply security patches to the underlying OS of Kubernetes nodes and any other VMs regularly. Use automated patching where possible.
    *   **Kubernetes Versions:** Plan for and execute Kubernetes version upgrades (control plane and nodes) to stay on supported versions and get new features/security fixes. This should be tested in Staging first.
    *   **Base Docker Images:** Regularly update the base images used for service containers to include the latest security patches. Rebuild and redeploy applications with updated base images.
    *   **Application Dependencies:** Regularly scan and update third-party libraries and dependencies for all microservices to address known vulnerabilities (using tools like Snyk, Dependabot, OWASP Dependency-Check).
    *   **Application Code:** Deploy bug fixes and minor improvements as needed through the CI/CD pipeline.

*   **Database Maintenance:**
    *   **Backups:** Implement automated daily backups for all databases (PostgreSQL, MongoDB). Ensure backups are stored securely (encrypted) and in a separate location (e.g., different region). Regularly test the restoration process.
    *   **Performance Tuning:** Monitor database performance (query execution times, index usage, connection pooling). Optimize slow queries, add/remove indexes as needed.
    *   **Capacity Management:** Monitor disk space, CPU, and memory usage. Plan for capacity upgrades or scaling as data grows.
    *   **Upgrades:** Plan and execute database engine version upgrades to stay supported and benefit from new features/performance improvements. Test thoroughly in Staging.

*   **Message Queue Maintenance:**
    *   **Monitoring:** Monitor queue depths, message rates, consumer acknowledgments, and resource usage of the message broker (RabbitMQ, Kafka).
    *   **Clustering/HA:** Ensure cluster health and data replication (e.g., mirrored queues in RabbitMQ) are functioning correctly.
    *   **Upgrades:** Plan and execute broker version upgrades.
    *   **Dead-Letter Queue (DLQ) Management:** Regularly review messages in DLQs to identify and address persistent processing failures.

*   **AFP Resource Management (Fonts, Overlays, Page Segments):**
    *   Establish a centralized and version-controlled repository for AFP resources (fonts, overlays, page segments, form definitions).
    *   Ensure the `AFPGenerationService` and `AFPConversionService` (or the underlying conversion tool) have access to the correct versions of these resources.
    *   Implement a process for updating these resources, which may involve updating configuration paths and potentially redeploying services if they cache these resources.

*   **Log Management and Archival:**
    *   **Rotation:** Implement log rotation for application and system logs on servers/containers to prevent disk space exhaustion.
    *   **Centralized Storage:** Ensure logs are shipped to a centralized logging system (ELK, Loki).
    *   **Archival:** Define log retention policies based on compliance and operational needs. Archive older logs from the hot storage of the centralized system to cheaper, long-term storage (e.g., AWS S3 Glacier, Azure Blob Archive).
    *   **Access Control:** Ensure access to archived logs is controlled and auditable.

*   **SSL/TLS Certificate Management:**
    *   **Monitoring Expiration:** Implement automated monitoring and alerting for SSL/TLS certificate expiration dates (for public-facing endpoints, internal mTLS certificates, etc.).
    *   **Automated Renewal:** Use tools like Let's Encrypt with cert-manager (for Kubernetes) to automate the issuance and renewal of publicly trusted TLS certificates.
    *   **Private CA:** For internal mTLS, use a private Certificate Authority (CA). Establish procedures for issuing and renewing service certificates.

## 3. System Monitoring

Consolidating discussions from previous design documents (especially `Batch_Processing_And_Reporting_Design.md`).

*   **Metrics Collection:**
    *   **Prometheus:** Primary tool for collecting time-series metrics from all microservices, Kubernetes, databases, and other infrastructure components. Services will expose a `/metrics` endpoint.
*   **Centralized Logging:**
    *   **ELK Stack (Elasticsearch, Logstash, Kibana) or Grafana Loki:** For collecting, storing, searching, and analyzing logs from all components.
*   **Dashboarding:**
    *   **Grafana:** To create dashboards for visualizing Prometheus metrics and logs from ELK/Loki. Dashboards will cover system health, service performance, business KPIs, and batch job status.
*   **Alerting:**
    *   **Alertmanager (Prometheus ecosystem):** For defining alerts based on metrics (e.g., high error rates, high latency, resource exhaustion, queue backlogs, failed batch jobs). Alerts routed to appropriate channels (email, Slack, PagerDuty).
    *   Alerts can also be configured based on log patterns in the centralized logging system.
*   **Application Performance Monitoring (APM):**
    *   **Need:** For deeper insights into application performance, distributed tracing across microservice calls, and identifying bottlenecks within application code.
    *   **Tools (Examples):** Jaeger (open source for tracing), Zipkin (open source for tracing), Dynatrace, New Relic, Datadog APM, Elastic APM.
    *   Integration with APM tools would involve instrumenting application code using APM agents or libraries.
*   **Key Areas to Monitor (Summary):**
    *   API Gateway: Request rates, error rates, latency.
    *   Core Services (AFP Gen, Conversion, Email/SMS/Push Senders, Dispatch, Customer Profile, Template Mgmt): Throughput, error rates, processing latency, resource utilization, queue depths.
    *   Batch Orchestrator (Airflow): DAG run status, task failures, scheduler health, worker availability.
    *   Databases: Query performance, connection counts, replication status, disk space, CPU/memory.
    *   Message Queues: Queue depths, message rates, consumer health.
    *   Infrastructure: Kubernetes cluster health, node resource utilization, network I/O.
    *   Security: Authentication failures, WAF alerts, IDS/IPS alerts.

## 4. Updates and Upgrades

Managing changes to the system once it's in production.

*   **Minor Updates vs. Major Version Upgrades:**
    *   **Minor Updates:** Bug fixes, small non-breaking feature enhancements, security patches. Deployed via the standard CI/CD pipeline using rolling updates. API version remains the same.
    *   **Major Version Upgrades:** Introduce breaking changes to APIs or significant changes in functionality.
        *   **API Versioning:** A new API version will be introduced (e.g., `/api/v2/...`). The old version (`/api/v1/...`) should be supported in parallel for a defined deprecation period to allow clients to migrate.
        *   **Deprecation Policy:** Clearly communicate API deprecation timelines and provide migration guides.
        *   Deployment might involve Blue/Green or Canary strategies for careful rollout.
*   **Database Schema Migrations:**
    *   **Tools:** Flyway (Java-focused) or Liquibase (more language-agnostic).
    *   **Process:**
        1.  Migration scripts are version-controlled alongside application code.
        2.  Migrations are applied automatically as part of the deployment process (often before the new application version starts or as a dedicated step in the pipeline).
        3.  Ensure migrations are backward-compatible if possible, or coordinate application and database deployments carefully to avoid downtime (e.g., expand-contract pattern for complex changes).
        4.  Test migrations thoroughly in Staging.
        5.  Have a rollback plan for failed migrations (though some migrations are hard to roll back directly and might require restoring from backup if catastrophic).
*   **Rollback Strategy:**
    *   **Automated Rollbacks (Kubernetes):** Kubernetes `Deployment` objects support automated rollbacks to a previous version if a new deployment fails health checks or is manually triggered.
    *   **CI/CD Pipeline Support:** The CI/CD pipeline should have a mechanism to easily redeploy a previous stable version.
    *   **Database Rollbacks:** More complex. For schema changes, it might involve applying a "down" migration script (if written) or restoring from backup. For data changes, specific rollback logic might be needed.
    *   **Blue/Green:** Simplifies rollback by just switching traffic back to the old environment.
    *   Test rollback procedures regularly.

## 5. Documentation

Comprehensive and up-to-date documentation is crucial for operations and maintenance. All documentation should be stored in a version-controlled repository (e.g., alongside the code or in a dedicated docs-as-code repository).

*   **System Architecture Document:** The overall design document you are currently creating, detailing microservices, their interactions, data flows, and technology choices.
*   **API Specifications:**
    *   OpenAPI 3.0 specifications for all external RESTful APIs.
    *   Internal API contracts if using different mechanisms (e.g., gRPC .proto files).
*   **Deployment Guides:**
    *   Step-by-step instructions for deploying the system (or individual services) to different environments.
    *   Kubernetes manifest details and configuration options.
    *   CI/CD pipeline setup and operation.
*   **Troubleshooting Guides / Runbooks:**
    *   Common issues, their symptoms, and steps to diagnose and resolve them.
    *   Procedures for handling common alerts.
    *   Contact information for different support levels/teams.
*   **Incident Response (IR) Plan:**
    *   Detailed plan as outlined in `Security_Considerations.md`.
*   **Maintenance Procedures:**
    *   Step-by-step instructions for routine maintenance tasks (patching, backups, certificate renewal, resource updates, etc.).
    *   Checklists for pre- and post-maintenance activities.
*   **User Manuals (for Visual Template Builder, Admin UIs):**
    *   Guides for end-users on how to use the system's interfaces.

This plan provides a framework for deploying and maintaining the system. It will require continuous refinement as the system is built and evolves.The `Deployment_and_Maintenance_Plan.md` document has been successfully created. It covers all the requested sections:

1.  **Deployment Strategy & Process:** Details on containerization (Docker), orchestration (Kubernetes), CI/CD pipeline stages and tools, production deployment strategies (Blue/Green, Canary, Rolling Update), Infrastructure as Code (Terraform), and Configuration Management (Kubernetes ConfigMaps/Secrets).
2.  **Ongoing Maintenance:** Strategies for regular patching (OS, Kubernetes, base images, dependencies), database maintenance, message queue maintenance, AFP resource management, log management/archival, and SSL/TLS certificate management.
3.  **System Monitoring:** Consolidation of monitoring tools (Prometheus, ELK/Loki, Grafana, Alertmanager), mention of APM, and key areas to monitor.
4.  **Updates and Upgrades:** Differentiation between minor updates and major version upgrades (including API versioning/deprecation), database schema migration strategies (Flyway/Liquibase), and rollback strategies.
5.  **Documentation:** Lists essential documentation types (system architecture, API specs, deployment/troubleshooting guides, IR plan, maintenance procedures, user manuals).

This document provides a clear operational framework for the system.

---
## 正體中文 (Traditional Chinese)

# 部署與維護計畫

本文件概述了 AFP 文件產生、轉換、交付和管理系統的部署與持續維護策略。目標是為可靠且有效率地部署、管理和更新系統建立明確的營運框架。

## 1. 部署策略與流程

將採用現代化的自動化部署方法，利用容器化、協同運作、CI/CD 和基礎架構即程式碼。

*   **容器化 (Docker)：**
    *   所有微服務 (核心 AFP 產生引擎、AFP 轉換模組、電子郵件傳送服務、簡訊傳送服務、應用程式推播服務、客戶設定檔服務、分派服務、API 閘道元件 (如果自行託管) 等) 都將打包為 Docker 容器。
    *   **優點：** 開發、測試和生產環境的一致性；簡化相依性管理；服務隔離；更快的部署。
    *   將為每個服務維護 Dockerfile，以定義其容器映像檔的建置流程。
    *   容器映像檔將儲存在私有容器登錄檔中 (例如 AWS ECR、Azure ACR、Google GCR、Harbor)。

*   **協同運作 (Kubernetes - K8s)：**
    *   Kubernetes 將用於協同運作容器化應用程式的部署、擴展和管理。
    *   **優點：** 自動化推出和復原、服務探索、負載平衡、自我修復 (重新啟動失敗的容器)、水平擴展、組態管理。
    *   將為每個微服務建立 Kubernetes 清單 (定義部署、服務、ConfigMap、Secret、Ingress 等的 YAML 檔案)。
    *   建議在雲端部署中使用受管理的 Kubernetes 服務 (EKS、AKS、GKE)，以減少營運開銷。

*   **CI/CD 管線：**
    *   健全的持續整合/持續交付 (CI/CD) 管線對於自動化且可靠的部署至關重要。
    *   **工具：** Jenkins、GitLab CI/CD、GitHub Actions、Azure DevOps Pipelines。
    *   **管線階段：**
        1.  **程式碼提交：** 開發人員將程式碼提交到 Git 儲存庫 (例如 GitLab、GitHub、Bitbucket)。
        2.  **建置與單元測試：**
            *   CI 伺服器觸發建置。
            *   原始碼已編譯。
            *   執行自動化單元測試。如果測試失敗，則建置失敗。
        3.  **靜態程式碼分析與安全性掃描 (SAST/SCA)：**
            *   分析程式碼的品質問題和已知漏洞 (例如 SonarQube、OWASP Dependency-Check)。
        4.  **容器化：**
            *   如果建置和測試通過，則建置 Docker 映像檔並加上標籤 (例如使用 Git 提交雜湊值和/或版本號碼)。
            *   映像檔已推送到私有容器登錄檔。
        5.  **部署到開發/測試環境：**
            *   將新映像檔自動部署到專用的開發或測試 Kubernetes 命名空間/叢集。
        6.  **自動化整合與 E2E 測試：**
            *   針對開發/測試環境中已部署的應用程式執行自動化整合測試和關鍵的端對端測試。
        7.  **部署到預備環境：**
            *   如果所有先前階段都通過，則自動或手動升級到預備環境 (生產複本)。
        8.  **使用者驗收測試 (UAT)：**
            *   利害關係人在預備環境中執行 UAT。
            *   也可能在此處執行效能和更廣泛的安全性測試 (例如 DAST)。
        9.  **核准生產部署：**
            *   根據成功的 UAT 和測試結果進行手動核准。
        10. **部署到生產環境：**
            *   使用選定的策略部署到生產 Kubernetes 叢集 (請參閱下文)。
        11. **部署後監控與冒煙測試：**
            *   部署後立即監控關鍵指標和日誌。
            *   執行自動化冒煙測試以驗證生產環境中的關鍵功能。

*   **生產部署策略：**
    *   **滾動更新 (預設 Kubernetes 策略)：**
        *   逐步將舊版應用程式執行個體替換為新版，一次一個或分批進行。
        *   如果新版本穩定，可確保零停機時間。
        *   可以使用整備度和活躍度探查進行設定，以確保新執行個體在流量轉移之前處於良好狀態。
        *   **建議：** 適用於大多數微服務更新。
    *   **藍/綠部署：**
        *   維護兩個相同的生產環境 (「藍色」和「綠色」)。任何時候只有一個環境處於活動狀態。
        *   將新版本部署到閒置環境 (例如綠色)。
        *   測試綠色環境後，將流量從藍色切換到綠色。
        *   **優點：** 如果發生問題，可透過將流量切換回藍色來立即復原。
        *   **缺點：** 需要雙倍的基礎架構資源。
        *   **建議：** 考慮用於關鍵服務或復原風險較高的主要更新。
    *   **金絲雀部署：**
        *   將新版本發行給一小部分使用者/流量。
        *   密切監控效能和錯誤。
        *   如果穩定，則逐步將新版本推廣給其餘使用者/流量。
        *   **優點：** 限制錯誤發行的影響。允許在小範圍內進行實際測試。
        *   **缺點：** 設定和管理流量路由更複雜 (通常需要像 Istio 或 Linkerd 這樣的服務網格，或進階的 Ingress 控制器功能)。
        *   **建議：** 進階策略，適用於高風險變更或進行 A/B 功能測試時。

*   **基礎架構即程式碼 (IaC)：**
    *   使用程式碼管理和佈建基礎架構 (VPC、子網路、Kubernetes 叢集、資料庫、訊息佇列、負載平衡器)。
    *   **工具：** Terraform (雲端無關)、AWS CloudFormation、Azure Resource Manager (ARM) 範本、Google Cloud Deployment Manager。
    *   **優點：** 可重複且一致的環境、基礎架構的版本控制、自動化佈建、減少人為錯誤。

*   **組態管理：**
    *   **Kubernetes ConfigMap：** 將非敏感組態資料 (例如應用程式設定、資源路徑、預設值) 儲存為 Kubernetes 中的 ConfigMap。這些可以檔案或環境變數的形式掛載到容器中。
    *   **Kubernetes Secret：** 將敏感組態資料 (API 金鑰、資料庫密碼、TLS 憑證、DKIM 私密金鑰) 儲存為 Kubernetes Secret。這些是 base64 編碼的 (預設情況下在 etcd 中並未真正靜態加密，因此請確保啟用 etcd 加密或使用外部機密管理器)。
    *   **外部機密管理 (建議用於更高安全性)：** 將 Kubernetes 與 HashiCorp Vault、AWS Secrets Manager 或 Azure Key Vault 等系統整合，以在執行階段安全地將機密注入 pod 中。這樣可以避免將敏感資料直接儲存在 Kubernetes etcd 中。

## 2. 持續維護

定期維護對於安全性、穩定性和效能至關重要。

*   **定期修補與更新：**
    *   **作業系統 (OS)：** 定期對 Kubernetes 節點和任何其他 VM 的底層作業系統套用安全性修補程式。盡可能使用自動化修補。
    *   **Kubernetes 版本：** 規劃並執行 Kubernetes 版本升級 (控制平面和節點)，以保持支援的版本並取得新功能/安全性修正。應先在預備環境中進行測試。
    *   **基礎 Docker 映像檔：** 定期更新用於服務容器的基礎映像檔，以包含最新的安全性修補程式。使用更新的基礎映像檔重新建置並重新部署應用程式。
    *   **應用程式相依性：** 定期掃描並更新所有微服務的第三方函式庫和相依性，以解決已知漏洞 (使用 Snyk、Dependabot、OWASP Dependency-Check 等工具)。
    *   **應用程式程式碼：** 視需要透過 CI/CD 管線部署錯誤修正和次要改良。

*   **資料庫維護：**
    *   **備份：** 為所有資料庫 (PostgreSQL、MongoDB) 實作自動化每日備份。確保備份安全儲存 (加密) 並位於不同位置 (例如不同區域)。定期測試還原程序。
    *   **效能調整：** 監控資料庫效能 (查詢執行時間、索引使用情況、連線池)。視需要最佳化緩慢查詢、新增/移除索引。
    *   **容量管理：** 監控磁碟空間、CPU 和記憶體使用情況。隨著資料成長規劃容量升級或擴展。
    *   **升級：** 規劃並執行資料庫引擎版本升級，以保持支援並受益於新功能/效能改良。在預備環境中進行徹底測試。

*   **訊息佇列維護：**
    *   **監控：** 監控佇列深度、訊息速率、取用者確認以及訊息代理程式 (RabbitMQ、Kafka) 的資源使用情況。
    *   **叢集/高可用性：** 確保叢集健康狀況和資料複製 (例如 RabbitMQ 中的鏡像佇列) 正常運作。
    *   **升級：** 規劃並執行代理程式版本升級。
    *   **無效信件佇列 (DLQ) 管理：** 定期檢閱 DLQ 中的訊息，以識別並解決持續的處理失敗。

*   **AFP 資源管理 (字型、疊加層、頁面區段)：**
    *   為 AFP 資源 (字型、疊加層、頁面區段、表單定義) 建立集中且版本控制的儲存庫。
    *   確保 `AFPGenerationService` 和 `AFPConversionService` (或底層轉換工具) 可以存取這些資源的正確版本。
    *   實作更新這些資源的程序，其中可能包括更新組態路徑，以及如果服務快取這些資源，則可能需要重新部署服務。

*   **日誌管理與封存：**
    *   **輪替：** 為伺服器/容器上的應用程式和系統日誌實作日誌輪替，以防止磁碟空間耗盡。
    *   **集中儲存：** 確保將日誌傳送到集中式日誌記錄系統 (ELK、Loki)。
    *   **封存：** 根據合規性和營運需求定義日誌保留原則。將較舊的日誌從集中式系統的熱儲存封存到更便宜的長期儲存 (例如 AWS S3 Glacier、Azure Blob 封存)。
    *   **存取控制：** 確保對封存日誌的存取受到控制且可稽核。

*   **SSL/TLS 憑證管理：**
    *   **監控到期日：** 為 SSL/TLS 憑證到期日 (用於公開端點、內部 mTLS 憑證等) 實作自動化監控和警示。
    *   **自動續約：** 使用 Let's Encrypt 搭配 cert-manager (適用於 Kubernetes) 等工具，自動化發行和續約公開信任的 TLS 憑證。
    *   **私有 CA：** 對於內部 mTLS，請使用私有憑證授權單位 (CA)。建立發行和續約服務憑證的程序。

## 3. 系統監控

整合先前設計文件 (尤其是 `Batch_Processing_And_Reporting_Design.md`) 的討論。

*   **指標收集：**
    *   **Prometheus：** 從所有微服務、Kubernetes、資料庫和其他基礎架構元件收集時間序列指標的主要工具。服務將公開 `/metrics` 端點。
*   **集中式日誌記錄：**
    *   **ELK Stack (Elasticsearch、Logstash、Kibana) 或 Grafana Loki：** 用於從所有元件收集、儲存、搜尋和分析日誌。
*   **儀表板：**
    *   **Grafana：** 建立儀表板以視覺化 Prometheus 指標和來自 ELK/Loki 的日誌。儀表板將涵蓋系統健康狀況、服務效能、業務 KPI 和批次工作狀態。
*   **警示：**
    *   **Alertmanager (Prometheus 生態系統)：** 用於根據指標定義警示 (例如高錯誤率、高延遲、資源耗盡、佇列待辦事項、失敗的批次工作)。警示會路由到適當的通道 (電子郵件、Slack、PagerDuty)。
    *   也可以根據集中式日誌記錄系統中的日誌模式設定警示。
*   **應用程式效能監控 (APM)：**
    *   **需求：** 更深入地了解應用程式效能、跨微服務呼叫的分散式追蹤以及識別應用程式程式碼中的瓶頸。
    *   **工具 (範例)：** Jaeger (用於追蹤的開源工具)、Zipkin (用於追蹤的開源工具)、Dynatrace、New Relic、Datadog APM、Elastic APM。
    *   與 APM 工具的整合將涉及使用 APM 代理程式或函式庫來檢測應用程式程式碼。
*   **要監控的關鍵領域 (摘要)：**
    *   API 閘道：請求速率、錯誤率、延遲。
    *   核心服務 (AFP 產生、轉換、電子郵件/簡訊/推播傳送器、分派、客戶設定檔、範本管理)：輸送量、錯誤率、處理延遲、資源利用率、佇列深度。
    *   批次協調器 (Airflow)：DAG 執行狀態、任務失敗、排程器健康狀況、worker 可用性。
    *   資料庫：查詢效能、連線計數、複製狀態、磁碟空間、CPU/記憶體。
    *   訊息佇列：佇列深度、訊息速率、取用者健康狀況。
    *   基礎架構：Kubernetes 叢集健康狀況、節點資源利用率、網路 I/O。
    *   安全性：驗證失敗、WAF 警示、IDS/IPS 警示。

## 4. 更新與升級

管理系統進入生產環境後的變更。

*   **次要更新與主要版本升級：**
    *   **次要更新：** 錯誤修正、小型非破壞性功能增強、安全性修補程式。透過標準 CI/CD 管線使用滾動更新進行部署。API 版本保持不變。
    *   **主要版本升級：** 對 API 引入破壞性變更或功能發生重大變更。
        *   **API 版本控制：** 將引入新的 API 版本 (例如 `/api/v2/...`)。舊版本 (`/api/v1/...`) 應在定義的棄用期間內平行支援，以允許用戶端遷移。
        *   **棄用原則：** 清楚傳達 API 棄用時間表並提供遷移指南。
        *   部署可能涉及藍/綠或金絲雀策略以進行謹慎的推出。
*   **資料庫結構描述遷移：**
    *   **工具：** Flyway (以 Java 為主) 或 Liquibase (更與語言無關)。
    *   **流程：**
        1.  遷移指令碼與應用程式程式碼一起進行版本控制。
        2.  遷移會作為部署程序的一部分自動套用 (通常在新應用程式版本啟動之前或作為管線中的專用步驟)。
        3.  如果可能，確保遷移具有向後相容性，或仔細協調應用程式和資料庫部署以避免停機時間 (例如，針對複雜變更採用擴展-收縮模式)。
        4.  在預備環境中徹底測試遷移。
        5.  為失敗的遷移制定復原計畫 (儘管某些遷移很難直接復原，如果發生災難性故障，可能需要從備份還原)。
*   **復原策略：**
    *   **自動復原 (Kubernetes)：** Kubernetes `Deployment` 物件支援在新部署未通過健康狀況檢查或手動觸發時自動復原到先前版本。
    *   **CI/CD 管線支援：** CI/CD 管線應具有輕鬆重新部署先前穩定版本的機制。
    *   **資料庫復原：** 更複雜。對於結構描述變更，可能涉及套用「向下」遷移指令碼 (如果已撰寫) 或從備份還原。對於資料變更，可能需要特定的復原邏輯。
    *   **藍/綠部署：** 只需將流量切換回舊環境即可簡化復原。
    *   定期測試復原程序。

## 5. 文件

全面且最新的文件對於營運和維護至關重要。所有文件都應儲存在版本控制的儲存庫中 (例如與程式碼一起或在專用的文件即程式碼儲存庫中)。

*   **系統架構文件：** 您目前正在建立的整體設計文件，詳細說明微服務、其互動、資料流程和技術選擇。
*   **API 規格：**
    *   所有外部 RESTful API 的 OpenAPI 3.0 規格。
    *   如果使用不同機制 (例如 gRPC .proto 檔案)，則為內部 API 合約。
*   **部署指南：**
    *   將系統 (或個別服務) 部署到不同環境的逐步說明。
    *   Kubernetes 清單詳細資訊和組態選項。
    *   CI/CD 管線設定與操作。
*   **疑難排解指南 / 操作手冊：**
    *   常見問題、其症狀以及診斷和解決這些問題的步驟。
    *   處理常見警示的程序。
    *   不同支援層級/團隊的聯絡資訊。
*   **事件應變 (IR) 計畫：**
    *   如 `Security_Considerations.md` 中所述的詳細計畫。
*   **維護程序：**
    *   例行維護任務 (修補、備份、憑證續約、資源更新等) 的逐步說明。
    *   維護前和維護後活動的檢查表。
*   **使用者手冊 (適用於視覺化範本產生器、管理 UI)：**
    *   關於如何使用系統介面的使用者指南。

此計畫為部署和維護系統提供了一個框架。隨著系統的建置和發展，它將需要不斷完善。
