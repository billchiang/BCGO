# Testing and Quality Assurance (QA) Strategy

This document outlines the comprehensive testing and quality assurance strategy for the AFP document generation, conversion, delivery, and management system. The goal is to ensure the system is reliable, performs as expected, is secure, and meets user requirements.

## 1. Testing Levels & Types

A multi-layered testing approach will be adopted, encompassing various levels and types of testing.

### a. Unit Testing

*   **Focus:** Testing individual components or units of code (e.g., a specific class, method, or function) in isolation. Verifying that each unit performs its specific function correctly.
*   **Responsibility:** Primarily developers writing the code.
*   **Tools:**
    *   Java: JUnit, Mockito, AssertJ
    *   Python: unittest, pytest, mock
    *   .NET: MSTest, NUnit, Moq
    *   JavaScript (for UI components): Jest, Mocha, Jasmine
*   **Coverage Examples:**
    *   Testing a data mapping function in the `AFPGenerationService` with various input data and expected output.
    *   Testing an MX record lookup function in the `EmailSenderService`.
    *   Testing a specific template variable substitution in the `TemplateManagementService`.
    *   Testing an AFP resource parsing utility.
    *   Verifying error handling for invalid inputs in an API controller.
    *   Aim for high code coverage (e.g., >80%) for critical components.

### b. Integration Testing

*   **Focus:** Testing the interaction between different components or microservices to ensure they work together as intended. Verifying data exchange, API calls, and communication via message queues.
*   **Responsibility:** Developers, with potential support from QA engineers.
*   **Tools:**
    *   Java: Spring Boot Test (for testing Spring components, controllers, service interactions), Testcontainers (for testing with real dependencies like databases or message queues in Docker containers).
    *   Python: pytest, requests (for API calls), Boto3 (for AWS services if applicable).
    *   .NET: Integration testing features in ASP.NET Core, Testcontainers.
    *   Message Queue Testing: Tools to publish/consume messages from RabbitMQ/Kafka and assert content/behavior.
*   **Examples:**
    *   Testing the flow from `AFPGenerationService` placing a message on a queue, to `AFPConversionService` picking it up and processing it.
    *   Testing an API call from the `BatchOrchestrationService` (e.g., Airflow DAG) to the `AFPGenerationService` API.
    *   Verifying that the `EmailSenderService` correctly retrieves DKIM keys and signs an email.
    *   Testing database interactions: ensuring a service correctly reads from or writes to a database schema.
    *   Testing interactions with the API Gateway for routing and basic auth.

### c. End-to-End (E2E) / System Testing

*   **Focus:** Testing the entire system workflow from the user's or external system's perspective. Verifying that all integrated components function correctly together to achieve a business objective. This simulates real user scenarios.
*   **Responsibility:** Primarily QA engineers, with support from developers for setup and debugging.
*   **Tools:**
    *   API Testing: Postman (manual and automated), RestAssured (Java), requests (Python), Cypress (can also do API tests).
    *   UI Testing (for Visual Template Builder): Selenium, Cypress, Playwright.
    *   Custom test scripts for orchestrating complex flows.
*   **Examples:**
    *   **Batch Processing E2E Test:**
        1.  Trigger a batch job via Airflow (or its API).
        2.  Verify that input data is correctly fetched.
        3.  Verify AFP files are generated and stored.
        4.  Verify AFP files are converted to the target format (e.g., PDF).
        5.  Verify emails are assembled with correct attachments/links.
        6.  Verify emails are sent (using a test SMTP server or mail trap service like MailHog/Mailtrap).
        7.  Verify status updates are correctly reflected via API or in the database.
    *   **Real-time API E2E Test:**
        1.  External system calls an API endpoint (e.g., to request a statement download or resend an email).
        2.  Verify the API Gateway routes the request correctly.
        3.  Verify the backend service processes the request.
        4.  Verify the expected output (file download, successful resend queueing).
        5.  Verify correct status codes and response bodies.
    *   **Visual Template Builder E2E Test:**
        1.  User creates a new template via UI.
        2.  User adds various elements (text, image placeholders, data placeholders).
        3.  User saves the template.
        4.  Verify the template is correctly stored via `TemplateManagementService` API.
        5.  Use this template in a test generation flow to ensure output matches design.

### d. Performance Testing

*   **Focus:** Evaluating the system's responsiveness, stability, and scalability under various load conditions.
*   **Responsibility:** Performance engineers or specialized QA engineers, with support from developers and DevOps.
*   **Tools:**
    *   Load Testing: Apache JMeter, K6, Gatling, Locust.
    *   Monitoring: Prometheus, Grafana, APM tools (e.g., Dynatrace, New Relic, Elastic APM).
*   **Types:**
    *   **Load Testing:** Simulate expected production load to verify system performance against defined SLAs (response times, throughput).
    *   **Stress Testing:** Push the system beyond its normal operating limits to identify breaking points, resource bottlenecks, and how it recovers.
    *   **Soak Testing (Endurance Testing):** Run the system under a sustained moderate load for an extended period to detect issues like memory leaks, resource exhaustion, or performance degradation over time.
    *   **Spike Testing:** Observe system behavior when subjected to sudden bursts of traffic.
*   **Key Metrics:**
    *   API Response Times (average, 95th/99th percentile).
    *   Throughput (requests per second, messages processed per minute).
    *   Error Rates under load.
    *   Resource Utilization (CPU, memory, network, disk I/O) of services, databases, message queues.
    *   Queue lengths and message processing latency.
    *   Database query performance.
*   **Examples:**
    *   Simulate 1000s of concurrent API requests to the API Gateway.
    *   Generate a large batch of 100,000 AFP generation requests and measure the time to completion and resource usage.
    *   Test the `EmailSenderService` with a high volume of emails to check sending rate and SMTP connection handling.
    *   Measure the performance of the AFP conversion process for large or complex AFP files.

### e. Security Testing

*   **Focus:** Identifying and mitigating security vulnerabilities in the system.
*   **Responsibility:** Security team or specialized security QA, often involving third-party penetration testers.
*   **Reference:** Detailed security measures are outlined in `Security_Considerations.md`.
*   **Types:**
    *   **Vulnerability Scanning:** Automated scanning of code (SAST), running applications (DAST), container images, and infrastructure.
    *   **Penetration Testing:** Authorized simulated attacks on the system to identify exploitable weaknesses.
    *   **API Security Testing:** Focus on authentication, authorization (scope enforcement), input validation for API endpoints, protection against common API attacks.
    *   **Security Audits:** Review of configurations, access controls, and compliance with security policies.
    *   **Dependency Scanning:** Checking third-party libraries for known vulnerabilities.

### f. Usability Testing

*   **Focus:** Evaluating the ease of use, intuitiveness, and user experience of the system's user interfaces, primarily the Visual Template Builder and any administrative dashboards.
*   **Responsibility:** UX designers and QA engineers, involving actual or representative users.
*   **Methods:**
    *   Conducting user testing sessions with predefined tasks.
    *   Gathering feedback through surveys, interviews, and observation.
    *   Heuristic evaluation against usability principles.
*   **Examples:**
    *   Can a user easily create a new AFP template with dynamic fields?
    *   Is the process for managing customer communication preferences clear?
    *   Can an operator easily monitor batch job status and identify errors in Airflow/Grafana?

### g. Compatibility Testing

*   **Focus:** Ensuring the system's outputs and interfaces work correctly across different environments and client software.
*   **Responsibility:** QA engineers.
*   **Types:**
    *   **AFP Viewer Compatibility:** Test generated AFP files with various common AFP viewers (e.g., IBM AFP Workbench, Ricoh ProcessDirector AFP Viewer, Papyrus AFP Viewer) to ensure correct rendering of text, images, overlays, and fonts.
    *   **PDF Reader Compatibility:** Test converted PDF/PDF/A files with different PDF readers (Adobe Acrobat Reader, common browser PDF viewers, Foxit Reader) for rendering accuracy and PDF/A compliance validation.
    *   **Browser Compatibility:** Test web UIs (Visual Template Builder, admin panels) across different major browsers (Chrome, Firefox, Edge, Safari) and versions.
    *   **Email Client Compatibility:** Test HTML emails rendered by different email clients (Outlook, Gmail, Apple Mail, web clients) as rendering can vary. (Tools like Litmus or Email on Acid can assist).

### h. Failover and Resilience Testing (Chaos Engineering)

*   **Focus:** Verifying the system's ability to withstand failures of components or infrastructure and recover gracefully.
*   **Responsibility:** DevOps, SREs, and QA engineers.
*   **Methods (Chaos Engineering principles):**
    *   Intentionally inject failures into a test environment (e.g., shut down a service instance, introduce network latency, make a database replica unavailable).
    *   Observe how the system responds:
        *   Does it failover to redundant components?
        *   Are alerts triggered?
        *   How does it impact overall system availability and data integrity?
        *   Does it recover automatically when the fault is removed?
*   **Tools:** Chaos Monkey (Netflix), Gremlin, custom scripts.
*   **Examples:**
    *   Simulate failure of a RabbitMQ node to ensure messages are not lost and consumers reconnect.
    *   Take down an instance of the `AFPConversionService` to see if requests are routed to other instances or queued appropriately.
    *   Simulate database failover.
    *   Test API Gateway's ability to handle backend service unavailability (e.g., circuit breaker pattern).

## 2. Test Environments

A series of isolated environments will be used for different stages of testing and development:

*   **Development (Dev):**
    *   Individual developer environments (local machines or cloud-based dev instances).
    *   Used for coding, unit testing, and initial component-level integration testing.
    *   May use mocks or lightweight local versions of dependent services (e.g., local RabbitMQ, in-memory DB).
*   **CI/Build Environment:**
    *   Used by the Continuous Integration server (e.g., Jenkins, GitLab CI, GitHub Actions).
    *   Automated builds, unit tests, and potentially some integration tests are run here on every code commit.
    *   May use Testcontainers to spin up ephemeral dependencies.
*   **QA/Testing Environment:**
    *   A stable environment deployed with the latest successful build from CI.
    *   Used by QA engineers for most functional testing, integration testing, E2E testing, and exploratory testing.
    *   Should be as close to production as possible in terms of configuration and integrated services (may use shared, but isolated, instances of databases, message queues).
*   **Staging/Pre-Production Environment:**
    *   A complete replica of the production environment (or as close as feasible).
    *   Used for final E2E testing, UAT, performance testing, and failover/resilience testing.
    *   Uses production-like data (anonymized if necessary).
    *   Deployments to production are typically promoted from this environment after successful validation.
*   **Production Environment:**
    *   The live system used by end-users and external systems.
    *   Monitoring and smoke testing are performed here, but extensive QA activities are avoided.

## 3. Test Data Management

Effective testing requires appropriate and well-managed test data.

*   **Need for Realistic and Diverse Data:**
    *   Test data must cover a wide range of scenarios, including valid inputs, invalid inputs, edge cases, and large volumes.
    *   For AFP generation, this includes diverse customer data, various template complexities, different image types, and font requirements.
*   **AFP Specific Data:**
    *   A library of sample AFP files with varying features (different MO:DCA levels, IOCA images, GOCA graphics, diverse font technologies, overlays, page segments, bar codes) will be maintained. This is crucial for testing the `AFPConversionService`.
    *   Sample XSL-FO files or template designs for testing the `AFPGenerationService`.
*   **Edge Cases:**
    *   Empty input files, very large files, files with missing resources, data with special characters, requests with missing optional fields, etc.
*   **PII (Personally Identifiable Information) Anonymization:**
    *   For testing in QA and Staging environments, production data (if used) **must be anonymized or masked** to protect PII and comply with data privacy regulations (GDPR, CCPA, etc.).
    *   Develop scripts or use tools to generate anonymized data sets that retain realistic characteristics.
*   **Tools/Scripts:**
    *   Data generation scripts (Python, Java) to create structured test data (JSON, CSV).
    *   Tools for database cloning/subsetting and anonymization.
    *   Version control test data alongside test scripts where appropriate.

## 4. Test Automation Strategy

Automation is key to achieving efficient and repeatable testing.

*   **Automate Extensively:**
    *   **Unit Tests:** Should be fully automated and cover a significant portion of the codebase.
    *   **Integration Tests:** Automate tests for API interactions, message queue communication, and database interactions.
    *   **E2E Tests:** Automate critical end-to-end user scenarios and API workflows.
    *   **Performance Tests:** Scripts for load, stress, and soak testing should be automated.
    *   **Security Scans:** Integrate automated security scanning tools into CI/CD.
*   **CI/CD Integration:**
    *   Automated tests (unit, integration) will be integrated into the CI/CD pipeline.
    *   Builds should fail if critical tests do not pass.
    *   Automated E2E tests can be triggered on deployment to QA/Staging environments.
*   **Test Pyramid Concept:**
    *   Emphasize a higher volume of unit tests at the base, followed by fewer integration tests, and even fewer E2E tests at the top.
    *   Unit tests are fast, cheap, and provide quick feedback. E2E tests are slower and more expensive but validate the entire system flow.

## 5. User Acceptance Testing (UAT)

*   **Focus:** Validating that the system meets the business requirements and is acceptable to the end-users or stakeholders.
*   **Responsibility:** Business analysts, product owners, and representatives from the end-user community, with support from QA and development teams.
*   **Process:**
    1.  Define UAT scope and criteria based on business requirements.
    2.  Prepare UAT test cases representing real-world user scenarios.
    3.  Conduct UAT in the Staging/Pre-Production environment.
    4.  Users execute test cases and provide feedback.
    5.  Defects found during UAT are logged and prioritized.
*   **Sign-off:** Formal sign-off from business stakeholders is required before deploying to production, indicating that the system meets their requirements.

## 6. Defect Management

A structured process for identifying, tracking, and resolving defects.

*   **Defect Tracking System:** Use a dedicated tool (e.g., Jira, Bugzilla, Azure DevOps Boards) to log and manage defects.
*   **Defect Lifecycle:** Define a clear lifecycle for defects (e.g., New, Open, Assigned, In Progress, Ready for Retest, Retest, Closed, Reopened, Deferred).
*   **Severity and Priority:**
    *   **Severity:** Impact of the defect on the system (e.g., Critical, Major, Minor, Trivial).
    *   **Priority:** Urgency of fixing the defect (e.g., High, Medium, Low).
    *   These are assigned during defect logging and reviewed during triage.
*   **Triage Process:** Regular meetings (e.g., daily or weekly) involving QA, Development, and Product Management to review new defects, assign severity/priority, and plan for resolution.

## 7. QA Team Roles and Responsibilities (Brief Outline)

*   **QA Lead/Manager:**
    *   Defines and oversees the overall QA strategy.
    *   Manages QA resources, planning, and reporting.
    *   Advocates for quality within the development lifecycle.
*   **QA Engineers (Manual & Automation):**
    *   Design, create, and execute test plans and test cases (manual and automated).
    *   Identify, log, and track defects.
    *   Develop and maintain test automation frameworks and scripts.
    *   Perform various types of testing (functional, integration, E2E, regression).
    *   Collaborate with developers to understand requirements and resolve issues.
*   **Performance Test Engineers (Specialized):**
    *   Design and execute performance tests.
    *   Analyze performance bottlenecks and provide recommendations.
*   **Security Test Engineers (Specialized):**
    *   Conduct security testing (vulnerability scans, support penetration tests).
    *   Advise on security best practices.

This testing and QA strategy provides a framework to ensure the delivery of a high-quality, reliable, and secure system. It will be a living document, reviewed and updated as the system evolves.

---
## 正體中文 (Traditional Chinese)

# 測試與品質保證 (QA) 策略

本文件概述了 AFP 文件產生、轉換、交付和管理系統的綜合測試與品質保證策略。目標是確保系統可靠、符合預期效能、安全並滿足使用者需求。

## 1. 測試層級與類型

將採用多層次的測試方法，包含各種測試層級和類型。

### a. 單元測試

*   **重點：** 在隔離環境中測試個別程式碼元件或單元 (例如特定的類別、方法或函數)。驗證每個單元是否正確執行其特定功能。
*   **職責：** 主要由撰寫程式碼的開發人員負責。
*   **工具：**
    *   Java：JUnit、Mockito、AssertJ
    *   Python：unittest、pytest、mock
    *   .NET：MSTest、NUnit、Moq
    *   JavaScript (用於 UI 元件)：Jest、Mocha、Jasmine
*   **涵蓋範圍範例：**
    *   使用各種輸入資料和預期輸出來測試 `AFPGenerationService` 中的資料對應功能。
    *   測試 `EmailSenderService` 中的 MX 記錄查閱功能。
    *   測試 `TemplateManagementService` 中的特定範本變數替換。
    *   測試 AFP 資源剖析公用程式。
    *   驗證 API 控制器中無效輸入的錯誤處理。
    *   目標是針對關鍵元件達到高程式碼覆蓋率 (例如 >80%)。

### b. 整合測試

*   **重點：** 測試不同元件或微服務之間的互動，以確保它們按預期協同運作。驗證資料交換、API 呼叫以及透過訊息佇列的通訊。
*   **職責：** 開發人員，可能由 QA 工程師支援。
*   **工具：**
    *   Java：Spring Boot Test (用於測試 Spring 元件、控制器、服務互動)、Testcontainers (用於在 Docker 容器中測試真實相依性，如資料庫或訊息佇列)。
    *   Python：pytest、requests (用於 API 呼叫)、Boto3 (如果適用於 AWS 服務)。
    *   .NET：ASP.NET Core 中的整合測試功能、Testcontainers。
    *   訊息佇列測試：用於從 RabbitMQ/Kafka 發佈/取用訊息並斷言內容/行為的工具。
*   **範例：**
    *   測試從 `AFPGenerationService` 將訊息放置到佇列，到 `AFPConversionService` 拾取並處理該訊息的流程。
    *   測試從 `BatchOrchestrationService` (例如 Airflow DAG) 到 `AFPGenerationService` API 的 API 呼叫。
    *   驗證 `EmailSenderService` 是否正確擷取 DKIM 金鑰並簽署電子郵件。
    *   測試資料庫互動：確保服務正確讀取或寫入資料庫結構描述。
    *   測試與 API 閘道的互動，以進行路由和基本驗證。

### c. 端對端 (E2E) / 系統測試

*   **重點：** 從使用者或外部系統的角度測試整個系統工作流程。驗證所有整合元件是否能正確協同運作以達成業務目標。這會模擬真實的使用者情境。
*   **職責：** 主要由 QA 工程師負責，開發人員提供設定和偵錯支援。
*   **工具：**
    *   API 測試：Postman (手動和自動化)、RestAssured (Java)、requests (Python)、Cypress (也可以執行 API 測試)。
    *   UI 測試 (用於視覺化範本產生器)：Selenium、Cypress、Playwright。
    *   用於協調複雜流程的自訂測試腳本。
*   **範例：**
    *   **批次處理 E2E 測試：**
        1.  透過 Airflow (或其 API) 觸發批次工作。
        2.  驗證輸入資料是否已正確擷取。
        3.  驗證 AFP 檔案是否已產生並儲存。
        4.  驗證 AFP 檔案是否已轉換為目標格式 (例如 PDF)。
        5.  驗證電子郵件是否已使用正確的附件/連結進行組合。
        6.  驗證電子郵件是否已傳送 (使用測試 SMTP 伺服器或郵件陷阱服務，如 MailHog/Mailtrap)。
        7.  驗證狀態更新是否已透過 API 或在資料庫中正確反映。
    *   **即時 API E2E 測試：**
        1.  外部系統呼叫 API 端點 (例如請求下載帳單或重新傳送電子郵件)。
        2.  驗證 API 閘道是否正確路由請求。
        3.  驗證後端服務是否處理請求。
        4.  驗證預期的輸出 (檔案下載、成功重新傳送佇列)。
        5.  驗證正確的狀態碼和回應內文。
    *   **視覺化範本產生器 E2E 測試：**
        1.  使用者透過 UI 建立新範本。
        2.  使用者新增各種元素 (文字、影像預留位置、資料預留位置)。
        3.  使用者儲存範本。
        4.  透過 `TemplateManagementService` API 驗證範本是否已正確儲存。
        5.  在測試產生流程中使用此範本，以確保輸出符合設計。

### d. 效能測試

*   **重點：** 評估系統在各種負載條件下的回應能力、穩定性和可擴展性。
*   **職責：** 效能工程師或專門的 QA 工程師，由開發人員和 DevOps 支援。
*   **工具：**
    *   負載測試：Apache JMeter、K6、Gatling、Locust。
    *   監控：Prometheus、Grafana、APM 工具 (例如 Dynatrace、New Relic、Elastic APM)。
*   **類型：**
    *   **負載測試：** 模擬預期的生產負載，以根據定義的 SLA (回應時間、輸送量) 驗證系統效能。
    *   **壓力測試：** 將系統推向超出其正常運作極限，以識別故障點、資源瓶頸以及系統如何復原。
    *   **浸泡測試 (耐久性測試)：** 在持續的中等負載下長時間執行系統，以偵測記憶體洩漏、資源耗盡或效能隨時間下降等問題。
    *   **尖峰測試：** 觀察系統在遭受突發流量時的行為。
*   **關鍵指標：**
    *   API 回應時間 (平均值、第 95/99 百分位數)。
    *   輸送量 (每秒請求數、每分鐘處理的訊息數)。
    *   負載下的錯誤率。
    *   服務、資料庫、訊息佇列的資源利用率 (CPU、記憶體、網路、磁碟 I/O)。
    *   佇列長度和訊息處理延遲。
    *   資料庫查詢效能。
*   **範例：**
    *   模擬對 API 閘道的數千個並行 API 請求。
    *   產生包含 100,000 個 AFP 產生請求的大型批次，並衡量完成時間和資源使用情況。
    *   使用大量電子郵件測試 `EmailSenderService`，以檢查傳送速率和 SMTP 連線處理。
    *   衡量大型或複雜 AFP 檔案的 AFP 轉換程序效能。

### e. 安全性測試

*   **重點：** 識別並緩解系統中的安全性漏洞。
*   **職責：** 安全性團隊或專門的安全性 QA，通常涉及第三方滲透測試人員。
*   **參考：** 詳細的安全性措施概述於 `Security_Considerations.md`。
*   **類型：**
    *   **漏洞掃描：** 自動掃描程式碼 (SAST)、執行中應用程式 (DAST)、容器映像檔和基礎架構。
    *   **滲透測試：** 對系統進行授權的模擬攻擊，以識別可利用的弱點。
    *   **API 安全性測試：** 著重於 API 端點的驗證、授權 (範圍強制執行)、輸入驗證，以及防禦常見的 API 攻擊。
    *   **安全性稽核：** 檢閱組態、存取控制以及是否符合安全性原則。
    *   **相依性掃描：** 檢查第三方函式庫是否存在已知漏洞。

### f. 可用性測試

*   **重點：** 評估系統使用者介面 (主要是視覺化範本產生器和任何管理儀表板) 的易用性、直覺性和使用者體驗。
*   **職責：** UX 設計師和 QA 工程師，涉及實際或具代表性的使用者。
*   **方法：**
    *   使用預先定義的任務進行使用者測試。
    *   透過問卷、訪談和觀察收集回饋。
    *   根據可用性原則進行啟發式評估。
*   **範例：**
    *   使用者是否可以輕鬆地使用動態欄位建立新的 AFP 範本？
    *   管理客戶通訊偏好設定的流程是否清楚？
    *   操作員是否可以在 Airflow/Grafana 中輕鬆監控批次工作狀態並識別錯誤？

### g. 相容性測試

*   **重點：** 確保系統的輸出和介面在不同環境和用戶端軟體之間能正確運作。
*   **職責：** QA 工程師。
*   **類型：**
    *   **AFP 檢視器相容性：** 使用各種常見的 AFP 檢視器 (例如 IBM AFP Workbench、Ricoh ProcessDirector AFP Viewer、Papyrus AFP Viewer) 測試產生的 AFP 檔案，以確保文字、影像、疊加層和字型的正確呈現。
    *   **PDF 閱讀器相容性：** 使用不同的 PDF 閱讀器 (Adobe Acrobat Reader、常見瀏覽器 PDF 檢視器、Foxit Reader) 測試轉換後的 PDF/PDF/A 檔案，以確保呈現準確性和 PDF/A 合規性驗證。
    *   **瀏覽器相容性：** 在不同的主要瀏覽器 (Chrome、Firefox、Edge、Safari) 及其版本上測試 Web UI (視覺化範本產生器、管理面板)。
    *   **電子郵件用戶端相容性：** 測試不同電子郵件用戶端 (Outlook、Gmail、Apple Mail、Web 用戶端) 呈現的 HTML 電子郵件，因為呈現方式可能有所不同。(Litmus 或 Email on Acid 等工具可以提供協助)。

### h. 容錯移轉與彈性測試 (混沌工程)

*   **重點：** 驗證系統承受元件或基礎架構故障並優雅復原的能力。
*   **職責：** DevOps、SRE 和 QA 工程師。
*   **方法 (混沌工程原則)：**
    *   故意將故障注入測試環境 (例如關閉服務執行個體、引入網路延遲、使資料庫複本無法使用)。
    *   觀察系統如何回應：
        *   是否容錯移轉到備援元件？
        *   是否觸發警示？
        *   這對整體系統可用性和資料完整性有何影響？
        *   移除故障後是否會自動復原？
*   **工具：** Chaos Monkey (Netflix)、Gremlin、自訂腳本。
*   **範例：**
    *   模擬 RabbitMQ 節點故障，以確保訊息不會遺失且取用者會重新連線。
    *   關閉 `AFPConversionService` 的一個執行個體，以查看請求是否路由到其他執行個體或適當排入佇列。
    *   模擬資料庫容錯移轉。
    *   測試 API 閘道處理後端服務無法使用的能力 (例如斷路器模式)。

## 2. 測試環境

將使用一系列隔離的環境進行不同階段的測試和開發：

*   **開發 (Dev) 環境：**
    *   個別開發人員環境 (本機或雲端型開發執行個體)。
    *   用於編碼、單元測試和初始元件層級的整合測試。
    *   可能使用模擬物件或相依服務的輕量級本機版本 (例如本機 RabbitMQ、記憶體內資料庫)。
*   **CI/建置環境：**
    *   由持續整合伺服器 (例如 Jenkins、GitLab CI、GitHub Actions) 使用。
    *   每次程式碼提交時，在此處執行自動化建置、單元測試以及可能的某些整合測試。
    *   可能使用 Testcontainers 來啟動臨時的相依性。
*   **QA/測試環境：**
    *   一個穩定的環境，部署了來自 CI 的最新成功建置。
    *   QA 工程師用於大多數功能測試、整合測試、E2E 測試和探索性測試。
    *   應盡可能接近生產環境的組態和整合服務 (可能使用共用但隔離的資料庫、訊息佇列執行個體)。
*   **預備/預生產環境：**
    *   生產環境的完整複本 (或盡可能接近)。
    *   用於最終的 E2E 測試、UAT、效能測試和容錯移轉/彈性測試。
    *   使用類似生產的資料 (如有必要，進行匿名化處理)。
    *   在成功驗證後，通常會從此環境升級部署到生產環境。
*   **生產環境：**
    *   供終端使用者和外部系統使用的即時系統。
    *   在此處執行監控和冒煙測試，但避免進行廣泛的 QA 活動。

## 3. 測試資料管理

有效的測試需要適當且管理良好的測試資料。

*   **對真實且多樣化資料的需求：**
    *   測試資料必須涵蓋廣泛的情境，包括有效輸入、無效輸入、邊緣案例和大量資料。
    *   對於 AFP 產生，這包括多樣化的客戶資料、各種範本複雜性、不同的影像類型和字型要求。
*   **AFP 特定資料：**
    *   將維護一個包含具有不同特性 (不同 MO:DCA 等級、IOCA 影像、GOCA 圖形、多種字型技術、疊加層、頁面區段、條碼) 的範例 AFP 檔案庫。這對於測試 `AFPConversionService` 至關重要。
    *   用於測試 `AFPGenerationService` 的範例 XSL-FO 檔案或範本設計。
*   **邊緣案例：**
    *   空輸入檔案、非常大的檔案、缺少資源的檔案、包含特殊字元的資料、缺少選用欄位的請求等。
*   **PII (個人識別資訊) 匿名化：**
    *   為了在 QA 和預備環境中進行測試，生產資料 (如果使用) **必須進行匿名化或遮罩**，以保護 PII 並遵守資料隱私法規 (GDPR、CCPA 等)。
    *   開發腳本或使用工具產生保留真實特性的匿名化資料集。
*   **工具/腳本：**
    *   用於建立結構化測試資料 (JSON、CSV) 的資料產生腳本 (Python、Java)。
    *   用於資料庫複製/子集化和匿名化的工具。
    *   在適當時，將測試資料與測試腳本一起進行版本控制。

## 4. 測試自動化策略

自動化是實現高效且可重複測試的關鍵。

*   **廣泛自動化：**
    *   **單元測試：** 應完全自動化並涵蓋大部分程式碼庫。
    *   **整合測試：** 自動化 API 互動、訊息佇列通訊和資料庫互動的測試。
    *   **E2E 測試：** 自動化關鍵的端對端使用者情境和 API 工作流程。
    *   **效能測試：** 負載、壓力和浸泡測試的腳本應自動化。
    *   **安全性掃描：** 將自動化安全性掃描工具整合到 CI/CD 管線中。
*   **CI/CD 整合：**
    *   自動化測試 (單元、整合) 將整合到 CI/CD 管線中。
    *   如果關鍵測試未通過，建置應失敗。
    *   可以在部署到 QA/預備環境時觸發自動化 E2E 測試。
*   **測試金字塔概念：**
    *   強調在底部進行大量單元測試，其次是較少的整合測試，頂部則是更少的 E2E 測試。
    *   單元測試快速、成本低廉並提供快速回饋。E2E 測試速度較慢且成本較高，但可以驗證整個系統流程。

## 5. 使用者驗收測試 (UAT)

*   **重點：** 驗證系統是否符合業務需求並且使用者或利害關係人可以接受。
*   **職責：** 業務分析師、產品負責人以及來自終端使用者社群的代表，由 QA 和開發團隊支援。
*   **流程：**
    1.  根據業務需求定義 UAT 範圍和標準。
    2.  準備代表真實世界使用者情境的 UAT 測試案例。
    3.  在預備/預生產環境中進行 UAT。
    4.  使用者執行測試案例並提供回饋。
    5.  UAT 期間發現的缺陷會被記錄並排定優先順序。
*   **簽核：** 在部署到生產環境之前，需要業務利害關係人的正式簽核，表示系統符合其要求。

## 6. 缺陷管理

用於識別、追蹤和解決缺陷的結構化流程。

*   **缺陷追蹤系統：** 使用專用工具 (例如 Jira、Bugzilla、Azure DevOps Boards) 記錄和管理缺陷。
*   **缺陷生命週期：** 為缺陷定義明確的生命週期 (例如新增、開啟、已指派、進行中、準備重新測試、重新測試、已關閉、重新開啟、已延遲)。
*   **嚴重性與優先順序：**
    *   **嚴重性：** 缺陷對系統的影響 (例如嚴重、主要、次要、輕微)。
    *   **優先順序：** 修復缺陷的急迫性 (例如高、中、低)。
    *   這些在缺陷記錄期間指派，並在分類會議期間檢閱。
*   **分類流程：** 定期會議 (例如每日或每週)，由 QA、開發和產品管理人員參與，以檢閱新的缺陷、指派嚴重性/優先順序並規劃解決方案。

## 7. QA 團隊角色與職責 (簡要概述)

*   **QA 主管/經理：**
    *   定義並監督整體 QA 策略。
    *   管理 QA 資源、規劃和報告。
    *   在開發生命週期中倡導品質。
*   **QA 工程師 (手動與自動化)：**
    *   設計、建立和執行測試計畫與測試案例 (手動和自動化)。
    *   識別、記錄和追蹤缺陷。
    *   開發和維護測試自動化框架與腳本。
    *   執行各種類型的測試 (功能、整合、E2E、迴歸)。
    *   與開發人員協作以了解需求並解決問題。
*   **效能測試工程師 (專業)：**
    *   設計和執行效能測試。
    *   分析效能瓶頸並提供建議。
*   **安全性測試工程師 (專業)：**
    *   進行安全性測試 (漏洞掃描、支援滲透測試)。
    *   提供安全性最佳實務建議。

此測試與 QA 策略提供了一個框架，以確保交付高品質、可靠且安全的系統。它將是一份動態文件，隨著系統的發展而檢閱和更新。
