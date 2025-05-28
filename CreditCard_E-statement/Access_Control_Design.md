# Access Control Design

This document outlines the access control mechanisms for the CreditCard E-statement system, covering external RESTful APIs, internal service-to-service communication, and administrative user access to UIs and tools.

## 1. Introduction & Goals

*   **Purpose:** To define robust strategies for authenticating entities (users, client applications, services) and authorizing their access to system resources and functionalities.
*   **Goals:**
    *   Prevent unauthorized access to sensitive data and system operations.
    *   Enforce the principle of least privilege, granting only necessary permissions.
    *   Ensure accountability by clearly identifying who performed what action.
    *   Provide a flexible framework that can adapt to evolving security requirements.
*   **Scope:**
    *   Authentication and authorization for external client systems accessing RESTful APIs.
    *   Authentication and authorization for internal service-to-service communication.
    *   Authentication and authorization for administrative users accessing system UIs (e.g., Visual Template Builder, operational dashboards, Airflow UI).

## 2. Authentication Mechanisms

Authentication is the process of verifying the identity of a user, client, or service.

### a. External RESTful APIs (Client Systems)

*   **Primary Mechanism: OAuth 2.0 Client Credentials Grant Flow**
    *   **Rationale:** This flow is well-suited for machine-to-machine (M2M) communication where external systems (clients) act on their own behalf without a direct human user context.
    *   **Process:**
        1.  Clients are pre-registered with the system's Authorization Server and receive a `client_id` and `client_secret`.
        2.  Clients request an access token from the Authorization Server's token endpoint by presenting their `client_id` and `client_secret`.
*   **Access Tokens: JSON Web Tokens (JWTs)**
    *   **Format:** The access tokens issued by the Authorization Server will be JWTs.
    *   **Content:** JWTs will contain standard claims (e.g., `iss` - issuer, `sub` - subject (client_id), `aud` - audience (our API), `exp` - expiration time, `iat` - issued at, `jti` - JWT ID) and custom claims, notably `scope` (for permissions).
    *   **Validation:** The API Gateway will be responsible for validating incoming JWTs by:
        *   Verifying the token's signature using the Authorization Server's public key.
        *   Checking the token's expiration (`exp`) and not-before (`nbf`, if used) times.
        *   Validating the issuer (`iss`) and audience (`aud`) claims.

### b. Internal Service-to-Service Communication

*   **Primary Recommendation: Mutual TLS (mTLS)**
    *   **Rationale:** Provides strong, certificate-based mutual authentication between services. Each microservice presents a client certificate to others it communicates with, and vice-versa.
    *   **Implementation:** Requires a Public Key Infrastructure (PKI) to issue and manage certificates for each service. Service mesh technologies (e.g., Istio, Linkerd) can simplify mTLS implementation and management.
*   **Alternative: OAuth 2.0 Client Credentials with JWTs**
    *   **Rationale:** If mTLS is too complex to implement initially, services can act as OAuth clients, obtaining JWTs from the Authorization Server scoped for specific internal operations.
    *   **Considerations:** Requires careful management of client credentials for each service and appropriate internal scope definitions.

### c. Administrative Users (UIs & Management Tools)

*   **Primary Mechanism: Single Sign-On (SSO) via SAML 2.0 or OpenID Connect (OIDC)**
    *   **Rationale:** Centralizes user identity management, improves user experience, and allows leveraging existing enterprise identity providers (IdPs).
    *   **Integration:** System UIs (Visual Template Builder, admin panels for Airflow, Grafana, etc.) will be configured as Service Providers (SPs) or Relying Parties (RPs) to integrate with an IdP (e.g., Keycloak, Okta, Azure AD, Google Workspace).
*   **Multi-Factor Authentication (MFA):**
    *   MFA MUST be enforced for all administrative users and users with access to sensitive functions or data. This should be configured at the IdP level.
*   **Local User Accounts:** Direct local user accounts in individual tools (e.g., Airflow, Grafana) should be minimized and used only for essential bootstrap or emergency access, with strong password policies and MFA where supported. SSO should be the default.

## 3. Authorization Mechanisms

Authorization is the process of determining whether an authenticated entity has permission to perform a specific action or access a particular resource.

### a. External RESTful APIs (Scope-Based Access Control - SBAC)

*   **OAuth Scopes:**
    *   **Definition:** Granular permissions defined as strings (e.g., `statement.read.status`, `statement.file.download`, `communication.job.submit`, `template.manage`).
    *   **Assignment:** API clients are granted specific scopes based on their intended functionality during registration with the Authorization Server.
    *   **Request:** Clients can request specific scopes when obtaining an access token (though the server may grant a subset).
    *   **Enforcement:** The API Gateway (or the backend service itself as a secondary check) will verify that the `scope` claim in the validated JWT contains the required scope(s) for the requested API endpoint and HTTP method. If not, a `403 Forbidden` error is returned.

### b. Role-Based Access Control (RBAC)

RBAC will be applied for administrative users accessing UIs and can also complement SBAC for APIs.

*   **Roles:**
    *   Define roles based on job functions or levels of system access. Examples:
        *   `SystemAdministrator`: Full control over the system.
        *   `OperationsUser`: Can monitor jobs, trigger resends, manage operational tasks.
        *   `TemplateDesigner`: Can create, edit, and manage communication templates.
        *   `SecurityAuditor`: Read-only access to security logs and configurations.
        *   `ApiClientManager`: Can register and manage API client credentials and their scopes.
        *   `ReportViewer`: Can access and view generated reports.
*   **Permissions:**
    *   Each role is associated with a set of specific permissions. A permission could be the ability to invoke a particular API (equivalent to an API scope) or access a specific UI feature.
*   **User/Client Assignment:**
    *   Administrative users are assigned roles, typically via group memberships synchronized from the IdP.
    *   API clients can also be associated with roles, which can determine the maximum set of scopes they can be granted.
*   **Enforcement:**
    *   **UIs:** Application backends for UIs will check the authenticated user's roles/permissions before rendering features or allowing actions.
    *   **APIs:** The API Gateway or backend services can use role information (potentially embedded in the JWT as a `roles` or `groups` claim, or looked up based on `client_id`/`user_id`) to make more complex authorization decisions beyond simple scope matching.

### c. Attribute-Based Access Control (ABAC) - Future Consideration

*   **Concept:** Authorization decisions are based on policies that evaluate attributes of the subject (user/client), resource, action, and environment.
*   **Potential Use:** For highly dynamic and fine-grained authorization scenarios (e.g., "Allow access to customer X's statements only if the API client belongs to organization Y and the request originates from a trusted IP range").
*   **Complexity:** ABAC is powerful but significantly more complex to design, implement, and manage than RBAC or SBAC. It can be considered for future enhancements if needed.

## 4. Authorization Server / Identity Provider (IdP)

A central component is responsible for authentication and issuing tokens/assertions.

*   **Responsibilities:**
    *   Managing API client registrations (`client_id`, `client_secret`).
    *   Defining and assigning OAuth scopes to clients.
    *   Authenticating clients (for Client Credentials flow) and users (via SAML/OIDC integration with enterprise IdPs).
    *   Issuing JWT access tokens for APIs.
    *   Issuing SAML assertions or OIDC ID tokens for user UI sessions.
    *   Potentially managing user identities and role/group memberships if a dedicated internal IdP is used (like Keycloak).
*   **Recommended Solutions:**
    *   **Keycloak:** Robust open-source Identity and Access Management solution. Supports OAuth 2.0, OpenID Connect, SAML, user federation, etc.
    *   **Commercial IdPs:** Okta, Auth0, Ping Identity, Azure AD, Google Cloud Identity Platform.
    *   The choice depends on existing enterprise infrastructure, feature requirements, and budget.

## 5. Implementation Considerations & Enforcement Points

*   **API Gateway:** The primary enforcement point for external API authentication (JWT validation) and coarse-grained authorization (scope validation).
*   **Microservice Level Authorization:**
    *   Individual microservices SHOULD perform secondary authorization checks, especially for operations requiring resource-specific permissions (e.g., checking if the authenticated client or user has rights to access a specific `statement_id` or `customer_id`).
    *   Services can extract claims (e.g., `client_id`, `user_id`, scopes, roles) from the JWT (propagated by the API Gateway) to make these decisions. This adheres to the principle of defense in depth.
*   **Policy Definition and Management:**
    *   Clearly define and document all roles, permissions, and OAuth scopes.
    *   Establish a process for managing and updating these policies as the system evolves.
    *   Store policies in a version-controlled manner where possible.
*   **Auditing:**
    *   All authentication attempts (success and failure) and authorization decisions (access granted or denied) MUST be logged comprehensively, as detailed in `Logging_And_Auditing_Strategy.md`.

## 6. Review and Updates

*   Access control policies, roles, permissions, and scope definitions MUST be reviewed regularly (e.g., quarterly or annually) and updated in response to:
    *   Changes in business requirements.
    *   Evolution of the system architecture.
    *   Emerging security threats or best practices.
    *   Findings from security audits or penetration tests.

This Access Control Design provides a framework for securing the system. Specific implementation details will depend on the chosen Authorization Server/IdP and the capabilities of the API Gateway and microservice frameworks.

---
## 正體中文 (Traditional Chinese)

# 存取控制設計

本文件概述了信用卡電子帳單系統的存取控制機制，涵蓋外部 RESTful API、內部服務對服務通訊以及管理使用者對 UI 和工具的存取。

## 1. 簡介與目標

*   **目的：** 為驗證實體 (使用者、用戶端應用程式、服務) 並授權其存取系統資源和功能定義健全的策略。
*   **目標：**
    *   防止未經授權存取敏感資料和系統操作。
    *   強制執行最小權限原則，僅授予必要的權限。
    *   透過清楚識別執行動作的人員來確保問責制。
    *   提供一個可適應不斷變化的安全性需求的彈性框架。
*   **範圍：**
    *   存取 RESTful API 的外部用戶端系統的驗證與授權。
    *   內部服務對服務通訊的驗證與授權。
    *   存取系統 UI (例如視覺化範本產生器、營運儀表板、Airflow UI) 的管理使用者的驗證與授權。

## 2. 驗證機制

驗證是驗證使用者、用戶端或服務身分的程序。

### a. 外部 RESTful API (用戶端系統)

*   **主要機制：OAuth 2.0 用戶端憑證授予流程**
    *   **理由：** 此流程非常適合機器對機器 (M2M) 通訊，其中外部系統 (用戶端) 代表自己行動，而沒有直接的人類使用者情境。
    *   **流程：**
        1.  用戶端已向系統的授權伺服器預先註冊，並收到 `client_id` 和 `client_secret`。
        2.  用戶端透過提交其 `client_id` 和 `client_secret` 向授權伺服器的權杖端點請求存取權杖。
*   **存取權杖：JSON Web Tokens (JWTs)**
    *   **格式：** 授權伺服器發行的存取權杖將是 JWT。
    *   **內容：** JWT 將包含標準宣告 (例如 `iss` - 發行者、`sub` - 主體 (client_id)、`aud` - 目標對象 (我們的 API)、`exp` - 到期時間、`iat` - 發行時間、`jti` - JWT ID) 和自訂宣告，尤其是 `scope` (用於權限)。
    *   **驗證：** API 閘道將負責透過以下方式驗證傳入的 JWT：
        *   使用授權伺服器的公開金鑰驗證權杖的簽章。
        *   檢查權杖的到期時間 (`exp`) 和生效時間 (`nbf`，如果使用)。
        *   驗證發行者 (`iss`) 和目標對象 (`aud`) 宣告。

### b. 內部服務對服務通訊

*   **主要建議：相互 TLS (mTLS)**
    *   **理由：** 在服務之間提供強大的、基於憑證的相互驗證。每個微服務都會向與其通訊的其他服務出示用戶端憑證，反之亦然。
    *   **實作：** 需要公開金鑰基礎結構 (PKI) 來為每個服務發行和管理憑證。服務網格技術 (例如 Istio、Linkerd) 可以簡化 mTLS 的實作和管理。
*   **替代方案：使用 JWT 的 OAuth 2.0 用戶端憑證**
    *   **理由：** 如果 mTLS 最初實作起來過於複雜，服務可以充當 OAuth 用戶端，從授權伺服器取得針對特定內部操作限定範圍的 JWT。
    *   **考量：** 需要仔細管理每個服務的用戶端憑證和適當的內部範圍定義。

### c. 管理使用者 (UI 與管理工具)

*   **主要機制：透過 SAML 2.0 或 OpenID Connect (OIDC) 的單一登入 (SSO)**
    *   **理由：** 集中化使用者身分管理、改善使用者體驗，並允許利用現有的企業身分識別提供者 (IdP)。
    *   **整合：** 系統 UI (視覺化範本產生器、Airflow 的管理面板、Grafana 等) 將設定為服務提供者 (SP) 或信賴方 (RP)，以與 IdP (例如 Keycloak、Okta、Azure AD、Google Workspace) 整合。
*   **多重要素驗證 (MFA)：**
    *   必須對所有管理使用者以及有權存取敏感功能或資料的使用者強制執行 MFA。這應在 IdP 層級進行設定。
*   **本機使用者帳戶：** 應盡量減少個別工具 (例如 Airflow、Grafana) 中的直接本機使用者帳戶，並且僅用於必要的啟動或緊急存取，並採用強式密碼原則和支援 MFA 的情況。SSO 應為預設值。

## 3. 授權機制

授權是確定已驗證實體是否有權執行特定動作或存取特定資源的程序。

### a. 外部 RESTful API (基於範圍的存取控制 - SBAC)

*   **OAuth 範圍：**
    *   **定義：** 定義為字串的細微性權限 (例如 `statement.read.status`、`statement.file.download`、`communication.job.submit`、`template.manage`)。
    *   **指派：** API 用戶端在向授權伺服器註冊期間，會根據其預期功能授予特定範圍。
    *   **請求：** 用戶端在取得存取權杖時可以請求特定範圍 (儘管伺服器可能會授予子集)。
    *   **強制執行：** API 閘道 (或後端服務本身作為次要檢查) 將驗證已驗證 JWT 中的 `scope` 宣告是否包含所請求 API 端點和 HTTP 方法所需的範圍。如果沒有，則傳回 `403 Forbidden` 錯誤。

### b. 基於角色的存取控制 (RBAC)

RBAC 將套用於存取 UI 的管理使用者，也可以補充 API 的 SBAC。

*   **角色：**
    *   根據職務功能或系統存取層級定義角色。範例：
        *   `SystemAdministrator`：對系統的完整控制權。
        *   `OperationsUser`：可以監控工作、觸發重新傳送、管理營運任務。
        *   `TemplateDesigner`：可以建立、編輯和管理通訊範本。
        *   `SecurityAuditor`：對安全性記錄和組態的唯讀存取權。
        *   `ApiClientManager`：可以註冊和管理 API 用戶端憑證及其範圍。
        *   `ReportViewer`：可以存取和檢視產生的報告。
*   **權限：**
    *   每個角色都與一組特定權限相關聯。權限可以是叫用特定 API (相當於 API 範圍) 或存取特定 UI 功能的能力。
*   **使用者/用戶端指派：**
    *   管理使用者會被指派角色，通常是透過從 IdP 同步的群組成員資格。
    *   API 用戶端也可以與角色相關聯，這可以決定它們可以被授予的最大範圍集合。
*   **強制執行：**
    *   **UI：** UI 的應用程式後端將在呈現功能或允許動作之前檢查已驗證使用者的角色/權限。
    *   **API：** API 閘道或後端服務可以使用角色資訊 (可能嵌入在 JWT 中作為 `roles` 或 `groups` 宣告，或根據 `client_id`/`user_id` 查閱) 來做出超出簡單範圍比對的更複雜授權決策。

### c. 基於屬性的存取控制 (ABAC) - 未來考量

*   **概念：** 授權決策基於評估主體 (使用者/用戶端)、資源、動作和環境屬性的原則。
*   **潛在用途：** 用於高度動態和細微性的授權情境 (例如，「僅當 API 用戶端屬於組織 Y 且請求源自受信任的 IP 範圍時，才允許存取客戶 X 的帳單」)。
*   **複雜性：** ABAC 功能強大，但設計、實作和管理比 RBAC 或 SBAC 複雜得多。如果需要，可以考慮將其用於未來的增強功能。

## 4. 授權伺服器 / 身分識別提供者 (IdP)

負責驗證和發行權杖/判斷提示的中央元件。

*   **職責：**
    *   管理 API 用戶端註冊 (`client_id`、`client_secret`)。
    *   定義 OAuth 範圍並將其指派給用戶端。
    *   驗證用戶端 (針對用戶端憑證流程) 和使用者 (透過與企業 IdP 的 SAML/OIDC 整合)。
    *   為 API 發行 JWT 存取權杖。
    *   為使用者 UI 工作階段發行 SAML 判斷提示或 OIDC ID 權杖。
    *   如果使用專用內部 IdP (如 Keycloak)，則可能管理使用者身分和角色/群組成員資格。
*   **建議解決方案：**
    *   **Keycloak：** 強大的開源身分和存取管理解決方案。支援 OAuth 2.0、OpenID Connect、SAML、使用者同盟等。
    *   **商業 IdP：** Okta、Auth0、Ping Identity、Azure AD、Google Cloud Identity Platform。
    *   選擇取決於現有的企業基礎結構、功能需求和預算。

## 5. 實作考量與強制執行點

*   **API 閘道：** 外部 API 驗證 (JWT 驗證) 和粗略授權 (範圍驗證) 的主要強制執行點。
*   **微服務層級授權：**
    *   個別微服務應執行次要授權檢查，特別是對於需要資源特定權限的操作 (例如，檢查已驗證用戶端或使用者是否有權存取特定的 `statement_id` 或 `customer_id`)。
    *   服務可以從 JWT (由 API 閘道傳播) 中擷取宣告 (例如 `client_id`、`user_id`、範圍、角色) 以做出這些決策。這符合深度防禦原則。
*   **原則定義與管理：**
    *   清楚定義和記錄所有角色、權限和 OAuth 範圍。
    *   建立一個程序來管理和更新這些原則，以因應系統的發展。
    *   盡可能以版本控制方式儲存原則。
*   **稽核：**
    *   所有驗證嘗試 (成功和失敗) 和授權決策 (授予或拒絕存取) 都必須全面記錄，如 `Logging_And_Auditing_Strategy.md` 中所述。

## 6. 檢閱與更新

*   存取控制原則、角色、權限和範圍定義必須定期檢閱 (例如每季或每年)，並根據以下情況進行更新：
    *   業務需求的變化。
    *   系統架構的演變。
    *   新出現的安全性威脅或最佳實務。
    *   安全性稽核或滲투測試的結果。

此存取控制設計為保護系統提供了一個框架。具體的實作細節將取決於所選的授權伺服器/IdP 以及 API 閘道和微服務框架的功能。
