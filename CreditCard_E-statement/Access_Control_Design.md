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
