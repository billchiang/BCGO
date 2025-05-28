# RESTful APIs for External System Integration - Design Document

## 1. API Design Principles

These principles will guide the development of all external-facing RESTful APIs to ensure consistency, usability, and maintainability.

*   **Adherence to REST Principles:**
    *   **Client-Server Architecture:** The client (external system) and server (our API) will evolve independently.
    *   **Statelessness:** Each request from the client to the server must contain all information needed to understand and process the request. The server will not store any client context between requests.
    *   **Cacheability:** Responses will be explicitly marked as cacheable or non-cacheable using HTTP cache headers (e.g., `Cache-Control`, `ETag`) where appropriate, especially for `GET` requests.
    *   **Uniform Interface:**
        *   **Resource-Based:** APIs will be designed around resources (e.g., statements, emails, batch_jobs).
        *   **Manipulation of Resources Through Representations:** Clients will interact with resources via their representations (JSON).
        *   **Standard HTTP Methods:** Use standard HTTP methods correctly:
            *   `GET`: Retrieve a resource or a collection of resources.
            *   `POST`: Create a new resource or trigger an action.
            *   `PUT`: Update an existing resource completely (not used in the currently defined endpoints but a general principle).
            *   `DELETE`: Remove a resource (not used in the currently defined endpoints).
            *   `PATCH`: Partially update an existing resource (not used in the currently defined endpoints).
        *   **Hypermedia as the Engine of Application State (HATEOAS):** While not strictly enforced for all initial endpoints to reduce complexity, responses may include links to related resources or actions where appropriate.
    *   **Layered System:** The client does not need to know if it's communicating directly with a service or through intermediaries like an API Gateway.
*   **JSON Usage:**
    *   **Request and Response Bodies:** JSON (application/json) will be the exclusive format for request and response bodies.
    *   **Naming Convention:** Use `snake_case` for JSON property keys to maintain consistency (e.g., `statement_id`, `created_at`).
*   **API Versioning:**
    *   **URL Path Versioning:** API versions will be included in the URL path (e.g., `/api/v1/statements/...`). This is explicit and easy for clients to manage.
    *   **Versioning Strategy:** Start with `v1`. Increment the version for breaking changes (e.g., changes to response structures, removal of fields, changes in endpoint behavior). Non-breaking changes (e.g., adding new optional fields or new endpoints) can be part of the same version.
*   **Naming Conventions:**
    *   **Resource URLs:** Use plural nouns for resource collections (e.g., `/statements`, `/emails`). Use resource IDs for specific instances (e.g., `/statements/{statement_id}`).
    *   **Paths:** Use lowercase letters and hyphens (`-`) to separate words in paths if needed, although `snake_case` or `camelCase` for path segments is also acceptable if consistently applied. For this document, we'll assume resource names are simple enough not to need separators beyond the `/`.
    *   **Query Parameters:** Use `snake_case` (e.g., `sort_by=date`, `page_size=20`).
*   **Standardized Error Handling:**
    *   **HTTP Status Codes:** Use standard HTTP status codes to indicate the outcome of a request.
        *   `2xx` for success (e.g., `200 OK`, `201 Created`, `202 Accepted`, `204 No Content`).
        *   `4xx` for client errors (e.g., `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `429 Too Many Requests`).
        *   `5xx` for server errors (e.g., `500 Internal Server Error`, `503 Service Unavailable`).
    *   **Error Response Body:** For `4xx` and `5xx` responses, provide a consistent JSON error body:
        ```json
        {
          "error": {
            "code": "ERROR_CODE_STRING", // e.g., "INVALID_INPUT", "AUTHENTICATION_FAILED"
            "message": "A human-readable description of the error.",
            "details": [ // Optional, for more specific error details
              {
                "field": "field_name", // If error is related to a specific input field
                "issue": "Description of the issue with the field."
              }
            ]
          }
        }
        ```

## 2. API Gateway Role

An API Gateway will serve as the single entry point for all external API requests.

*   **Responsibilities:**
    *   **Routing:** Route incoming API requests to the appropriate backend microservice or internal API endpoint. For example, a request to `/api/v1/statements/{id}/status` might be routed to a `StatusQueryService` or directly to the `AFPGenerationService` (if it exposes status).
    *   **Authentication and Authorization:**
        *   Verify authentication tokens (JWTs obtained via OAuth 2.0 Client Credentials flow).
        *   Enforce authorization based on validated token scopes, ensuring the client has permission to access the requested resource and perform the operation.
    *   **Rate Limiting:** Implement rate limiting policies (e.g., per client ID, per IP address) to protect backend services from abuse and ensure fair usage. If a client exceeds the rate limit, the gateway will return a `429 Too Many Requests` error.
    *   **Request/Response Transformation (Minimal):** While the gateway *can* transform requests/responses, this should be minimized. Backend services should ideally conform to the public API contract. Minor transformations like header manipulation might be acceptable.
    *   **Logging:** Log all requests and responses (headers, status codes, request IDs, client ID, timestamps, processing time). This is crucial for monitoring, auditing, and debugging. Sensitive information in bodies should be masked.
    *   **SSL/TLS Termination:** Terminate SSL/TLS connections from external clients. Communication between the gateway and internal microservices can also be over TLS for enhanced security within the internal network.
    *   **Caching (Optional):** The API Gateway can cache responses for certain `GET` requests to improve performance and reduce load on backend services, respecting cache headers.
*   **Recommended API Gateway Solutions:**
    *   Kong, Tyk, AWS API Gateway, Azure API Management, Spring Cloud Gateway (if Java-based). The choice depends on the deployment environment and existing infrastructure.

## 3. Detailed API Endpoint Specifications

All paths are prefixed with `/api/v1`.

---

### 1. Get Statement Status

*   **HTTP Method & URL Path:** `GET /statements/{statement_id}/status`
*   **Description:** Retrieves the current processing status of a specific statement.
*   **Path Parameters:**
    *   `statement_id` (string, required): The unique identifier of the statement.
*   **Query Parameters:** None.
*   **Request Body Schema:** None.
*   **Success Response Codes & Body Schema:**
    *   `200 OK`:
        ```json
        {
          "statement_id": "stmt_123abc",
          "status": "Processed", // e.g., "Pending", "Generating_AFP", "AFP_Generated", "Converting_To_PDF", "Converted_To_PDF", "Email_Queued", "Email_Sent", "Failed_Generation", "Failed_Conversion", "Failed_Email"
          "last_updated_at": "2025-01-15T10:30:00Z",
          "details": "Email successfully sent to recipient@example.com" // Optional, human-readable detail
        }
        ```
*   **Key Error Response Codes & Example Body:**
    *   `401 Unauthorized`: (See standardized error body in section 1)
    *   `403 Forbidden`: (See standardized error body)
    *   `404 Not Found`:
        ```json
        {
          "error": {
            "code": "STATEMENT_NOT_FOUND",
            "message": "Statement with ID 'stmt_xyz789' not found."
          }
        }
        ```

---

### 2. Get Email Status

*   **HTTP Method & URL Path:** `GET /emails/{email_id}/status`
*   **Description:** Retrieves the delivery status of a specific email associated with a statement. (Note: `email_id` might be the same as `statement_id` if one email per statement, or a unique ID if multiple emails/retries for a statement).
*   **Path Parameters:**
    *   `email_id` (string, required): The unique identifier of the email.
*   **Query Parameters:** None.
*   **Request Body Schema:** None.
*   **Success Response Codes & Body Schema:**
    *   `200 OK`:
        ```json
        {
          "email_id": "email_123abc",
          "statement_id": "stmt_123abc", // Optional, if relevant
          "status": "Sent", // e.g., "Queued", "Attempting_Send", "Sent", "Deferred", "Bounced_Hard", "Bounced_Soft", "Delivered_FBL_Spam"
          "recipient_address": "recipient@example.com",
          "sent_at": "2025-01-15T10:35:00Z", // If sent
          "last_event_at": "2025-01-15T10:35:00Z",
          "details": "SMTP Response: 250 OK" // Optional, details from EmailSenderService
        }
        ```
*   **Key Error Response Codes & Example Body:**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`:
        ```json
        {
          "error": {
            "code": "EMAIL_NOT_FOUND",
            "message": "Email with ID 'email_xyz789' not found."
          }
        }
        ```

---

### 3. Get Batch Job Status

*   **HTTP Method & URL Path:** `GET /batch_jobs/{batch_job_id}/status`
*   **Description:** Retrieves the current status of a specific batch processing job.
*   **Path Parameters:**
    *   `batch_job_id` (string, required): The unique identifier of the batch job.
*   **Query Parameters:** None.
*   **Request Body Schema:** None.
*   **Success Response Codes & Body Schema:**
    *   `200 OK`:
        ```json
        {
          "batch_job_id": "batch_daily_invoices_20250115",
          "status": "Completed", // e.g., "Running", "Completed", "Failed", "Partially_Completed"
          "submitted_at": "2025-01-15T02:00:00Z",
          "started_at": "2025-01-15T02:01:00Z", // Optional
          "completed_at": "2025-01-15T03:30:00Z", // Optional
          "progress": {
            "total_items": 1000,
            "processed_items": 1000,
            "failed_items": 5
          },
          "details": "5 items failed due to invalid customer data." // Optional
        }
        ```
*   **Key Error Response Codes & Example Body:**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`:
        ```json
        {
          "error": {
            "code": "BATCH_JOB_NOT_FOUND",
            "message": "Batch job with ID 'batch_xyz789' not found."
          }
        }
        ```

---

### 4. Download Statement Document

*   **HTTP Method & URL Path:** `GET /statements/{statement_id}/download`
*   **Description:** Downloads the generated document (e.g., PDF) for a specific statement.
*   **Path Parameters:**
    *   `statement_id` (string, required): The unique identifier of the statement.
*   **Query Parameters:**
    *   `format` (string, optional, default: "pdf"): Specifies the desired format if multiple are available (e.g., "pdf", "afp"). The system will primarily offer PDF for external download.
*   **Request Body Schema:** None.
*   **Success Response Codes & Body Schema:**
    *   `200 OK`: The response body will be the raw file content (e.g., binary PDF data).
        *   **Headers:**
            *   `Content-Type`: (e.g., `application/pdf`, `application/afp`)
            *   `Content-Disposition`: `attachment; filename="statement_{statement_id}.pdf"`
*   **Key Error Response Codes & Example Body:**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`: If the statement or the document in the specified format does not exist.
        ```json
        {
          "error": {
            "code": "DOCUMENT_NOT_FOUND",
            "message": "Document for statement ID 'stmt_xyz789' in format 'pdf' not found or not yet generated."
          }
        }
        ```
    *   `422 Unprocessable Entity`: If the document is not yet ready for download (e.g., still processing).
        ```json
        {
          "error": {
            "code": "DOCUMENT_NOT_READY",
            "message": "Document for statement ID 'stmt_xyz789' is still being processed."
          }
        }
        ```

---

### 5. Resend Email for a Statement

*   **HTTP Method & URL Path:** `POST /statements/{statement_id}/resend_email`
*   **Description:** Triggers a resend of the email for a specific statement. This might create a new email attempt or retry a previously failed one.
*   **Path Parameters:**
    *   `statement_id` (string, required): The unique identifier of the statement.
*   **Query Parameters:** None.
*   **Request Body Schema:** (Optional)
    ```json
    {
      "recipient_email_address": "new_recipient@example.com" // Optional: If resending to a different or corrected email address
    }
    ```
*   **Success Response Codes & Body Schema:**
    *   `202 Accepted`:
        ```json
        {
          "message_id": "new_email_id_456def", // ID of the new email job/request
          "statement_id": "stmt_123abc",
          "status": "Resend_Queued",
          "message": "Email resend request accepted and queued for processing."
        }
        ```
*   **Key Error Response Codes & Example Body:**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`: If the statement does not exist.
    *   `409 Conflict`: If the statement is in a state where email cannot be resent (e.g., AFP generation failed).
        ```json
        {
          "error": {
            "code": "CANNOT_RESEND_EMAIL",
            "message": "Cannot resend email for statement ID 'stmt_xyz789' as it is in 'Failed_Generation' state."
          }
        }
        ```
    *   `400 Bad Request`: If the optional `recipient_email_address` is invalid.

---

### 6. Reissue Statement (Full Reprocessing)

*   **HTTP Method & URL Path:** `POST /statements/reissue`
*   **Description:** Initiates a full reprocessing of one or more statements. This means regenerating AFP, converting, and sending emails as if it were a new request, potentially with updated data or templates.
*   **Path Parameters:** None.
*   **Query Parameters:** None.
*   **Request Body Schema:** (Required)
    ```json
    {
      "statement_ids": ["stmt_123abc", "stmt_456xyz"], // Array of statement IDs to reissue
      "reissue_reason": "Data correction batch X.", // Optional, for auditing
      "use_latest_template": true, // Optional, boolean, default: true. If true, uses the latest active template. If false, attempts to use the template version originally associated with the statement.
      "override_data_source_url": "s3://updated-data-bucket/batch_data.json" // Optional, URL to fetch new data for these statements. If not provided, system attempts to use original data sources.
    }
    ```
*   **Success Response Codes & Body Schema:**
    *   `202 Accepted`:
        ```json
        {
          "batch_reissue_id": "reissue_batch_789ghi", // ID for this batch reissue operation
          "status": "Reissue_Queued",
          "message": "Statement reissue request for 2 statements accepted and queued.",
          "details": [
            {"original_statement_id": "stmt_123abc", "new_statement_id": "stmt_new_001", "status": "Queued_For_Generation"},
            {"original_statement_id": "stmt_456xyz", "new_statement_id": "stmt_new_002", "status": "Queued_For_Generation"}
          ]
        }
        ```
*   **Key Error Response Codes & Example Body:**
    *   `400 Bad Request`: If `statement_ids` is empty or contains invalid IDs, or other input validation fails.
        ```json
        {
          "error": {
            "code": "INVALID_INPUT",
            "message": "Statement IDs list cannot be empty."
          }
        }
        ```
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`: If some of the provided statement IDs do not exist. The response might indicate which ones.

## 4. Authentication and Authorization Strategy

*   **Authentication: OAuth 2.0 Client Credentials Grant Flow**
    *   **Rationale:** This flow is suitable for machine-to-machine (M2M) communication where external systems (clients) act on their own behalf, without a direct user context.
    *   **Process:**
        1.  **Client Registration:** External clients are pre-registered with the OAuth 2.0 Authorization Server (which could be a component of the API Gateway or a dedicated identity provider like Keycloak, Okta, Auth0). Each client receives a `client_id` and a `client_secret`.
        2.  **Token Request:** The client makes a `POST` request to the Authorization Server's token endpoint (e.g., `/oauth/token`).
            *   `grant_type`: `client_credentials`
            *   `client_id`: Client's ID
            *   `client_secret`: Client's secret
            *   `scope`: (Optional) Space-separated list of requested scopes (e.g., `statement.status.read statement.file.download`).
        3.  **Token Response:** If credentials are valid, the Authorization Server returns a JSON response containing:
            *   `access_token`: A JWT (JSON Web Token).
            *   `token_type`: `Bearer`.
            *   `expires_in`: Token lifetime in seconds.
            *   `scope`: Granted scopes (may be a subset of requested scopes).
*   **Access Tokens: JSON Web Tokens (JWTs)**
    *   **Structure:** The `access_token` will be a JWT containing claims such as:
        *   `iss` (Issuer): URL of the Authorization Server.
        *   `sub` (Subject): The `client_id`.
        *   `aud` (Audience): Identifier of the resource server (our API Gateway or specific service).
        *   `exp` (Expiration Time): Timestamp when the token expires.
        *   `iat` (Issued At): Timestamp when the token was issued.
        *   `jti` (JWT ID): Unique token identifier.
        *   `scope`: Space-separated list of granted permissions/scopes.
    *   **Usage:** The client includes this JWT in the `Authorization` header of every API request:
        `Authorization: Bearer <jwt_access_token>`
    *   **Validation:** The API Gateway will validate the JWT signature, expiration, issuer, and audience before processing the request.
*   **Authorization: OAuth Scopes for Fine-Grained Access Control**
    *   Scopes define specific permissions that clients can be granted.
    *   **Example Scopes:**
        *   `statement.status.read`: Allows `GET /statements/{id}/status`.
        *   `email.status.read`: Allows `GET /emails/{id}/status`.
        *   `batchjob.status.read`: Allows `GET /batch_jobs/{id}/status`.
        *   `statement.file.download`: Allows `GET /statements/{id}/download`.
        *   `statement.email.resend`: Allows `POST /statements/{id}/resend_email`.
        *   `statement.reissue`: Allows `POST /statements/reissue`.
        *   `statement.admin.read`: Broader read access for internal/admin tools.
        *   `statement.admin.write`: Broader write access.
    *   **Enforcement:** The API Gateway (or underlying service if gateway delegates) will check if the `scope` claim in the validated JWT contains the required scope(s) for the requested endpoint and method. If not, a `403 Forbidden` error is returned.
    *   Clients request only the scopes they need. The Authorization Server may grant all or a subset of requested scopes based on the client's registration.

## 5. Documentation Strategy

*   **Standard: OpenAPI 3.0 (formerly Swagger)**
    *   All external APIs will be documented using the OpenAPI 3.0 specification.
    *   The OpenAPI document (e.g., `openapi.yaml` or `openapi.json`) will define:
        *   API general information (version, title, description).
        *   Server URLs.
        *   Paths (endpoints), including HTTP methods, parameters (path, query, header), request bodies, and responses.
        *   Reusable components (schemas for request/response bodies, security schemes).
        *   Security schemes (detailing the OAuth 2.0 Client Credentials flow).
*   **Interactive Documentation: Swagger UI / Redoc**
    *   The OpenAPI specification file will be used to generate interactive API documentation using tools like Swagger UI or Redoc.
    *   This allows developers (both internal and external integrators) to:
        *   Browse API endpoints and their details.
        *   View request/response schemas.
        *   **Try out API calls directly from the browser** (Swagger UI can facilitate this, often integrating with the OAuth flow for authorization).
    *   The interactive documentation will be hosted at a well-known URL (e.g., `/api/docs`).
*   **Content of Documentation:**
    *   Clear explanations of each endpoint's functionality.
    *   Detailed descriptions of all parameters and request/response fields.
    *   Example requests and responses.
    *   Explanation of authentication/authorization mechanisms.
    *   Error code explanations.
    *   Rate limiting information.
    *   Changelog for API versions.

## 6. Interaction with Backend Microservices

The API Gateway acts as a facade, abstracting the underlying microservice architecture from external clients.

*   **`GET /statements/{id}/status`:**
    *   Gateway validates token and scopes.
    *   Routes to a dedicated `StatusQueryService` or directly to the `AFPGenerationService` (or a service that consolidates status from various stages like generation, conversion, email).
    *   The backend service queries its database(s) to fetch the latest status for the given statement ID.
*   **`GET /emails/{id}/status`:**
    *   Gateway validates.
    *   Routes to `StatusQueryService` or `EmailSenderService`.
    *   Backend service queries its database for email status.
*   **`GET /batch_jobs/{id}/status`:**
    *   Gateway validates.
    *   Routes to the `BatchOrchestrationService` (e.g., Airflow's API or a wrapper around it).
    *   Backend service queries its job store for batch job status.
*   **`GET /statements/{id}/download`:**
    *   Gateway validates.
    *   Routes to a `DocumentRetrievalService` or the service responsible for storing the final converted documents (e.g., `AFPConversionService` might store them, or they might be moved to a dedicated document store).
    *   Backend service fetches the file from storage (e.g., S3, file system) and streams it back.
*   **`POST /statements/{id}/resend_email`:**
    *   Gateway validates.
    *   Routes to the `EmailSenderService` or an `ActionDispatchService`.
    *   The backend service:
        1.  Validates if the statement can be resent.
        2.  If a new recipient address is provided, it might update its records.
        3.  Queues a new email request to its internal sending queue, possibly linking it to the original statement.
*   **`POST /statements/reissue`:**
    *   Gateway validates.
    *   Routes to the `BatchOrchestrationService` (e.g., triggers a specific Airflow DAG).
    *   The `BatchOrchestrationService` then initiates the workflow:
        *   May call `AFPGenerationService` with parameters to re-fetch data or use new data, and regenerate AFPs.
        *   Subsequent steps (conversion, email) would follow as per the batch processing design, likely involving message queues between services.

A dedicated `StatusQueryService` could be beneficial to consolidate status information from various services if status tracking becomes complex and spread out. Initially, status queries might go directly to the service most authoritative for that particular status.The design document "RESTful APIs for External System Integration" has been created. It details API design principles (REST, JSON, versioning, naming, error handling), the API Gateway's role (routing, auth, rate limiting, logging), provides detailed specifications for six API endpoints, outlines an OAuth 2.0 and JWT-based authentication/authorization strategy with scopes, proposes OpenAPI 3.0 for documentation, and briefly describes how API calls interact with backend microservices.

---
## 正體中文 (Traditional Chinese)

# 外部系統整合的 RESTful API - 設計文件

## 1. API 設計原則

這些原則將指導所有外部 RESTful API 的開發，以確保一致性、可用性和可維護性。

*   **遵守 REST 原則：**
    *   **主從式架構：** 用戶端 (外部系統) 和伺服器 (我們的 API) 將獨立發展。
    *   **無狀態：** 從用戶端到伺服器的每個請求都必須包含理解和處理該請求所需的所有資訊。伺服器不會在請求之間儲存任何用戶端內容。
    *   **可快取性：** 在適當情況下，尤其對於 `GET` 請求，將使用 HTTP 快取標頭 (例如 `Cache-Control`、`ETag`) 明確標記回應為可快取或不可快取。
    *   **統一介面：**
        *   **基於資源：** API 將圍繞資源進行設計 (例如帳單、電子郵件、批次工作)。
        *   **透過表示形式操作資源：** 用戶端將透過其表示形式 (JSON) 與資源互動。
        *   **標準 HTTP 方法：** 正確使用標準 HTTP 方法：
            *   `GET`：擷取資源或資源集合。
            *   `POST`：建立新資源或觸發動作。
            *   `PUT`：完全更新現有資源 (目前定義的端點未使用，但為一般原則)。
            *   `DELETE`：移除資源 (目前定義的端點未使用)。
            *   `PATCH`：部分更新現有資源 (目前定義的端點未使用)。
        *   **超媒體即應用程式狀態引擎 (HATEOAS)：** 雖然為了降低複雜性，並非所有初始端點都嚴格強制執行，但回應在適當時可能包含相關資源或動作的連結。
    *   **分層系統：** 用戶端不需要知道它是直接與服務通訊還是透過 API 閘道等中介進行通訊。
*   **JSON 使用：**
    *   **請求與回應內文：** JSON (application/json) 將是請求和回應內文的唯一格式。
    *   **命名慣例：** JSON 屬性金鑰使用 `snake_case` 以維持一致性 (例如 `statement_id`、`created_at`)。
*   **API 版本控制：**
    *   **URL 路徑版本控制：** API 版本將包含在 URL 路徑中 (例如 `/api/v1/statements/...`)。這對用戶端而言明確且易於管理。
    *   **版本控制策略：** 從 `v1` 開始。對於重大變更 (例如回應結構變更、欄位移除、端點行為變更)，增加版本號。非重大變更 (例如新增選用欄位或新端點) 可以是相同版本的一部分。
*   **命名慣例：**
    *   **資源 URL：** 資源集合使用複數名詞 (例如 `/statements`、`/emails`)。特定執行個體使用資源 ID (例如 `/statements/{statement_id}`)。
    *   **路徑：** 如果需要，在路徑中使用小寫字母和連字號 (`-`) 分隔單字，但如果一致套用，路徑區段使用 `snake_case` 或 `camelCase` 也是可接受的。在本文件中，我們假設資源名稱足夠簡單，不需要 `/` 以外的分隔符號。
    *   **查詢參數：** 使用 `snake_case` (例如 `sort_by=date`、`page_size=20`)。
*   **標準化錯誤處理：**
    *   **HTTP 狀態碼：** 使用標準 HTTP 狀態碼表示請求的結果。
        *   `2xx` 表示成功 (例如 `200 OK`、`201 Created`、`202 Accepted`、`204 No Content`)。
        *   `4xx` 表示用戶端錯誤 (例如 `400 Bad Request`、`401 Unauthorized`、`403 Forbidden`、`404 Not Found`、`429 Too Many Requests`)。
        *   `5xx` 表示伺服器錯誤 (例如 `500 Internal Server Error`、`503 Service Unavailable`)。
    *   **錯誤回應內文：** 對於 `4xx` 和 `5xx` 回應，提供一致的 JSON 錯誤內文：
        ```json
        {
          "error": {
            "code": "ERROR_CODE_STRING", // 例如 "INVALID_INPUT"、"AUTHENTICATION_FAILED"
            "message": "A human-readable description of the error.",
            "details": [ // 選用，用於更具體的錯誤詳細資訊
              {
                "field": "field_name", // 如果錯誤與特定輸入欄位相關
                "issue": "Description of the issue with the field."
              }
            ]
          }
        }
        ```

## 2. API 閘道角色

API 閘道將作為所有外部 API 請求的單一進入點。

*   **職責：**
    *   **路由：** 將傳入的 API 請求路由到適當的後端微服務或內部 API 端點。例如，對 `/api/v1/statements/{id}/status` 的請求可能會路由到 `StatusQueryService` 或直接路由到 `AFPGenerationService` (如果它公開狀態)。
    *   **驗證與授權：**
        *   驗證驗證權杖 (透過 OAuth 2.0 用戶端憑證流程取得的 JWT)。
        *   根據已驗證的權杖範圍強制執行授權，確保用戶端有權存取請求的資源並執行操作。
    *   **速率限制：** 實作速率限制原則 (例如依用戶端 ID、依 IP 位址)，以保護後端服務免遭濫用並確保公平使用。如果用戶端超過速率限制，閘道將傳回 `429 Too Many Requests` 錯誤。
    *   **請求/回應轉換 (最小)：** 雖然閘道*可以*轉換請求/回應，但應盡量減少這種情況。後端服務最好符合公用 API 合約。次要轉換 (如標頭操作) 可能是可接受的。
    *   **記錄：** 記錄所有請求和回應 (標頭、狀態碼、請求 ID、用戶端 ID、時間戳記、處理時間)。這對於監控、稽核和偵錯至關重要。內文中的敏感資訊應進行遮罩。
    *   **SSL/TLS 終止：** 終止來自外部用戶端的 SSL/TLS 連線。閘道與內部微服務之間的通訊也可以透過 TLS 進行，以增強內部網路的安全性。
    *   **快取 (選用)：** API 閘道可以快取某些 `GET` 請求的回應，以提高效能並減少後端服務的負載，同時遵循快取標頭。
*   **建議的 API 閘道解決方案：**
    *   Kong、Tyk、AWS API Gateway、Azure API Management、Spring Cloud Gateway (如果基於 Java)。選擇取決於部署環境和現有基礎架構。

## 3. 詳細 API 端點規格

所有路徑都以 `/api/v1` 為前置詞。

---

### 1. 取得帳單狀態

*   **HTTP 方法與 URL 路徑：** `GET /statements/{statement_id}/status`
*   **描述：** 擷取特定帳單的目前處理狀態。
*   **路徑參數：**
    *   `statement_id` (字串，必要)：帳單的唯一識別碼。
*   **查詢參數：** 無。
*   **請求內文結構描述：** 無。
*   **成功回應碼與內文結構描述：**
    *   `200 OK`：
        ```json
        {
          "statement_id": "stmt_123abc",
          "status": "Processed", // 例如 "Pending"、"Generating_AFP"、"AFP_Generated"、"Converting_To_PDF"、"Converted_To_PDF"、"Email_Queued"、"Email_Sent"、"Failed_Generation"、"Failed_Conversion"、"Failed_Email"
          "last_updated_at": "2025-01-15T10:30:00Z",
          "details": "Email successfully sent to recipient@example.com" // 選用，人類可讀的詳細資訊
        }
        ```
*   **主要錯誤回應碼與範例內文：**
    *   `401 Unauthorized`：(請參閱第 1 節中的標準化錯誤內文)
    *   `403 Forbidden`：(請參閱標準化錯誤內文)
    *   `404 Not Found`：
        ```json
        {
          "error": {
            "code": "STATEMENT_NOT_FOUND",
            "message": "Statement with ID 'stmt_xyz789' not found."
          }
        }
        ```

---

### 2. 取得電子郵件狀態

*   **HTTP 方法與 URL 路徑：** `GET /emails/{email_id}/status`
*   **描述：** 擷取與帳單相關的特定電子郵件的傳送狀態。(注意：如果每個帳單只有一封電子郵件，則 `email_id` 可能與 `statement_id` 相同；如果一個帳單有多封電子郵件/重試，則為唯一 ID)。
*   **路徑參數：**
    *   `email_id` (字串，必要)：電子郵件的唯一識別碼。
*   **查詢參數：** 無。
*   **請求內文結構描述：** 無。
*   **成功回應碼與內文結構描述：**
    *   `200 OK`：
        ```json
        {
          "email_id": "email_123abc",
          "statement_id": "stmt_123abc", // 選用，如果相關
          "status": "Sent", // 例如 "Queued"、"Attempting_Send"、"Sent"、"Deferred"、"Bounced_Hard"、"Bounced_Soft"、"Delivered_FBL_Spam"
          "recipient_address": "recipient@example.com",
          "sent_at": "2025-01-15T10:35:00Z", // 如果已傳送
          "last_event_at": "2025-01-15T10:35:00Z",
          "details": "SMTP Response: 250 OK" // 選用，來自 EmailSenderService 的詳細資訊
        }
        ```
*   **主要錯誤回應碼與範例內文：**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`：
        ```json
        {
          "error": {
            "code": "EMAIL_NOT_FOUND",
            "message": "Email with ID 'email_xyz789' not found."
          }
        }
        ```

---

### 3. 取得批次工作狀態

*   **HTTP 方法與 URL 路徑：** `GET /batch_jobs/{batch_job_id}/status`
*   **描述：** 擷取特定批次處理工作的目前狀態。
*   **路徑參數：**
    *   `batch_job_id` (字串，必要)：批次工作的唯一識別碼。
*   **查詢參數：** 無。
*   **請求內文結構描述：** 無。
*   **成功回應碼與內文結構描述：**
    *   `200 OK`：
        ```json
        {
          "batch_job_id": "batch_daily_invoices_20250115",
          "status": "Completed", // 例如 "Running"、"Completed"、"Failed"、"Partially_Completed"
          "submitted_at": "2025-01-15T02:00:00Z",
          "started_at": "2025-01-15T02:01:00Z", // 選用
          "completed_at": "2025-01-15T03:30:00Z", // 選用
          "progress": {
            "total_items": 1000,
            "processed_items": 1000,
            "failed_items": 5
          },
          "details": "5 items failed due to invalid customer data." // 選用
        }
        ```
*   **主要錯誤回應碼與範例內文：**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`：
        ```json
        {
          "error": {
            "code": "BATCH_JOB_NOT_FOUND",
            "message": "Batch job with ID 'batch_xyz789' not found."
          }
        }
        ```

---

### 4. 下載帳單文件

*   **HTTP 方法與 URL 路徑：** `GET /statements/{statement_id}/download`
*   **描述：** 下載特定帳單產生的文件 (例如 PDF)。
*   **路徑參數：**
    *   `statement_id` (字串，必要)：帳單的唯一識別碼。
*   **查詢參數：**
    *   `format` (字串，選用，預設為 "pdf")：如果有多種格式可用 (例如 "pdf"、"afp")，則指定所需的格式。系統主要提供 PDF 供外部下載。
*   **請求內文結構描述：** 無。
*   **成功回應碼與內文結構描述：**
    *   `200 OK`：回應內文將是原始檔案內容 (例如二進位 PDF 資料)。
        *   **標頭：**
            *   `Content-Type`：(例如 `application/pdf`、`application/afp`)
            *   `Content-Disposition`：`attachment; filename="statement_{statement_id}.pdf"`
*   **主要錯誤回應碼與範例內文：**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`：如果帳單或指定格式的文件不存在。
        ```json
        {
          "error": {
            "code": "DOCUMENT_NOT_FOUND",
            "message": "Document for statement ID 'stmt_xyz789' in format 'pdf' not found or not yet generated."
          }
        }
        ```
    *   `422 Unprocessable Entity`：如果文件尚未準備好下載 (例如仍在處理中)。
        ```json
        {
          "error": {
            "code": "DOCUMENT_NOT_READY",
            "message": "Document for statement ID 'stmt_xyz789' is still being processed."
          }
        }
        ```

---

### 5. 重新傳送帳單電子郵件

*   **HTTP 方法與 URL 路徑：** `POST /statements/{statement_id}/resend_email`
*   **描述：** 觸發重新傳送特定帳單的電子郵件。這可能會建立新的電子郵件嘗試或重試先前失敗的嘗試。
*   **路徑參數：**
    *   `statement_id` (字串，必要)：帳單的唯一識別碼。
*   **查詢參數：** 無。
*   **請求內文結構描述：** (選用)
    ```json
    {
      "recipient_email_address": "new_recipient@example.com" // 選用：如果重新傳送到不同或更正的電子郵件地址
    }
    ```
*   **成功回應碼與內文結構描述：**
    *   `202 Accepted`：
        ```json
        {
          "message_id": "new_email_id_456def", // 新電子郵件工作/請求的 ID
          "statement_id": "stmt_123abc",
          "status": "Resend_Queued",
          "message": "Email resend request accepted and queued for processing."
        }
        ```
*   **主要錯誤回應碼與範例內文：**
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`：如果帳單不存在。
    *   `409 Conflict`：如果帳單處於無法重新傳送電子郵件的狀態 (例如 AFP 產生失敗)。
        ```json
        {
          "error": {
            "code": "CANNOT_RESEND_EMAIL",
            "message": "Cannot resend email for statement ID 'stmt_xyz789' as it is in 'Failed_Generation' state."
          }
        }
        ```
    *   `400 Bad Request`：如果選用的 `recipient_email_address` 無效。

---

### 6. 重新發出帳單 (完整重新處理)

*   **HTTP 方法與 URL 路徑：** `POST /statements/reissue`
*   **描述：** 起始一或多個帳單的完整重新處理。這表示重新產生 AFP、轉換並傳送電子郵件，如同新的請求一樣，可能使用更新的資料或範本。
*   **路徑參數：** 無。
*   **查詢參數：** 無。
*   **請求內文結構描述：** (必要)
    ```json
    {
      "statement_ids": ["stmt_123abc", "stmt_456xyz"], // 要重新發出的帳單 ID 陣列
      "reissue_reason": "Data correction batch X.", // 選用，用於稽核
      "use_latest_template": true, // 選用，布林值，預設為 true。如果為 true，則使用最新的作用中範本。如果為 false，則嘗試使用最初與帳單關聯的範本版本。
      "override_data_source_url": "s3://updated-data-bucket/batch_data.json" // 選用，用於擷取這些帳單新資料的 URL。如果未提供，系統會嘗試使用原始資料來源。
    }
    ```
*   **成功回應碼與內文結構描述：**
    *   `202 Accepted`：
        ```json
        {
          "batch_reissue_id": "reissue_batch_789ghi", // 此批次重新發出操作的 ID
          "status": "Reissue_Queued",
          "message": "Statement reissue request for 2 statements accepted and queued.",
          "details": [
            {"original_statement_id": "stmt_123abc", "new_statement_id": "stmt_new_001", "status": "Queued_For_Generation"},
            {"original_statement_id": "stmt_456xyz", "new_statement_id": "stmt_new_002", "status": "Queued_For_Generation"}
          ]
        }
        ```
*   **主要錯誤回應碼與範例內文：**
    *   `400 Bad Request`：如果 `statement_ids` 為空或包含無效 ID，或其他輸入驗證失敗。
        ```json
        {
          "error": {
            "code": "INVALID_INPUT",
            "message": "Statement IDs list cannot be empty."
          }
        }
        ```
    *   `401 Unauthorized`
    *   `403 Forbidden`
    *   `404 Not Found`：如果提供的某些帳單 ID 不存在。回應可能會指出哪些 ID 不存在。

## 4. 驗證與授權策略

*   **驗證：OAuth 2.0 用戶端憑證授予流程**
    *   **理由：** 此流程適用於機器對機器 (M2M) 通訊，其中外部系統 (用戶端) 代表自己行動，而沒有直接的使用者情境。
    *   **流程：**
        1.  **用戶端註冊：** 外部用戶端已向 OAuth 2.0 授權伺服器 (可以是 API 閘道的元件或像 Keycloak、Okta、Auth0 這樣的專用身分識別提供者) 預先註冊。每個用戶端都會收到 `client_id` 和 `client_secret`。
        2.  **權杖請求：** 用戶端向授權伺服器的權杖端點 (例如 `/oauth/token`) 發出 `POST` 請求。
            *   `grant_type`：`client_credentials`
            *   `client_id`：用戶端的 ID
            *   `client_secret`：用戶端的秘密金鑰
            *   `scope`：(選用) 以空格分隔的請求範圍清單 (例如 `statement.status.read statement.file.download`)。
        3.  **權杖回應：** 如果憑證有效，授權伺服器會傳回包含以下內容的 JSON 回應：
            *   `access_token`：一個 JWT (JSON Web Token)。
            *   `token_type`：`Bearer`。
            *   `expires_in`：權杖生命週期 (秒)。
            *   `scope`：授予的範圍 (可能是請求範圍的子集)。
*   **存取權杖：JSON Web Tokens (JWTs)**
    *   **結構：** `access_token` 將是一個 JWT，包含以下宣告：
        *   `iss` (發行者)：授權伺服器的 URL。
        *   `sub` (主體)：`client_id`。
        *   `aud` (目標對象)：資源伺服器 (我們的 API 閘道或特定服務) 的識別碼。
        *   `exp` (到期時間)：權杖到期的時間戳記。
        *   `iat` (發行時間)：權杖發行的時間戳記。
        *   `jti` (JWT ID)：唯一的權杖識別碼。
        *   `scope`：以空格分隔的授予權限/範圍清單。
    *   **用法：** 用戶端在每個 API 請求的 `Authorization` 標頭中包含此 JWT：
        `Authorization: Bearer <jwt_access_token>`
    *   **驗證：** API 閘道將在處理請求之前驗證 JWT 簽章、到期日、發行者和目標對象。
*   **授權：用於細微性存取控制的 OAuth 範圍**
    *   範圍定義可以授予用戶端的特定權限。
    *   **範例範圍：**
        *   `statement.status.read`：允許 `GET /statements/{id}/status`。
        *   `email.status.read`：允許 `GET /emails/{id}/status`。
        *   `batchjob.status.read`：允許 `GET /batch_jobs/{id}/status`。
        *   `statement.file.download`：允許 `GET /statements/{id}/download`。
        *   `statement.email.resend`：允許 `POST /statements/{id}/resend_email`。
        *   `statement.reissue`：允許 `POST /statements/reissue`。
        *   `statement.admin.read`：內部/管理工具的更廣泛讀取權限。
        *   `statement.admin.write`：更廣泛的寫入權限。
    *   **強制執行：** API 閘道 (或底層服務，如果閘道委派) 將檢查已驗證 JWT 中的 `scope` 宣告是否包含請求端點和方法所需的範圍。如果沒有，則傳回 `403 Forbidden` 錯誤。
    *   用戶端僅請求其需要的範圍。授權伺服器可能會根據用戶端的註冊授予所有或部分請求的範圍。

## 5. 文件策略

*   **標準：OpenAPI 3.0 (前身為 Swagger)**
    *   所有外部 API 都將使用 OpenAPI 3.0 規格進行記錄。
    *   OpenAPI 文件 (例如 `openapi.yaml` 或 `openapi.json`) 將定義：
        *   API 一般資訊 (版本、標題、描述)。
        *   伺服器 URL。
        *   路徑 (端點)，包括 HTTP 方法、參數 (路徑、查詢、標頭)、請求內文和回應。
        *   可重複使用的元件 (請求/回應內文的結構描述、安全性結構描述)。
        *   安全性結構描述 (詳細說明 OAuth 2.0 用戶端憑證流程)。
*   **互動式文件：Swagger UI / Redoc**
    *   OpenAPI 規格檔案將用於使用 Swagger UI 或 Redoc 等工具產生互動式 API 文件。
    *   這允許開發人員 (內部和外部整合者) 能夠：
        *   瀏覽 API 端點及其詳細資訊。
        *   檢視請求/回應結構描述。
        *   **直接從瀏覽器試用 API 呼叫** (Swagger UI 可以促進這一點，通常與 OAuth 流程整合以進行授權)。
    *   互動式文件將託管在眾所周知的 URL (例如 `/api/docs`)。
*   **文件內容：**
    *   清楚解釋每個端點的功能。
    *   所有參數和請求/回應欄位的詳細描述。
    *   範例請求和回應。
    *   驗證/授權機制的說明。
    *   錯誤碼說明。
    *   速率限制資訊。
    *   API 版本的變更日誌。

## 6. 與後端微服務的互動

API 閘道充當外觀，將底層微服務架構與外部用戶端隔離開來。

*   **`GET /statements/{id}/status`：**
    *   閘道驗證權杖和範圍。
    *   路由到專用的 `StatusQueryService` 或直接路由到 `AFPGenerationService` (或從各個階段 (如產生、轉換、電子郵件) 整合狀態的服務)。
    *   後端服務查詢其資料庫以擷取給定帳單 ID 的最新狀態。
*   **`GET /emails/{id}/status`：**
    *   閘道驗證。
    *   路由到 `StatusQueryService` 或 `EmailSenderService`。
    *   後端服務查詢其資料庫以取得電子郵件狀態。
*   **`GET /batch_jobs/{id}/status`：**
    *   閘道驗證。
    *   路由到 `BatchOrchestrationService` (例如 Airflow 的 API 或其包裝器)。
    *   後端服務查詢其工作儲存庫以取得批次工作狀態。
*   **`GET /statements/{id}/download`：**
    *   閘道驗證。
    *   路由到 `DocumentRetrievalService` 或負責儲存最終轉換文件的服務 (例如 `AFPConversionService` 可能會儲存它們，或者它們可能會移至專用文件儲存庫)。
    *   後端服務從儲存庫 (例如 S3、檔案系統) 擷取檔案並將其串流傳回。
*   **`POST /statements/{id}/resend_email`：**
    *   閘道驗證。
    *   路由到 `EmailSenderService` 或 `ActionDispatchService`。
    *   後端服務：
        1.  驗證帳單是否可以重新傳送。
        2.  如果提供了新的收件人電子郵件地址，它可能會更新其記錄。
        3.  將新的電子郵件請求排入其內部傳送佇列，並可能將其連結到原始帳單。
*   **`POST /statements/reissue`：**
    *   閘道驗證。
    *   路由到 `BatchOrchestrationService` (例如觸發特定的 Airflow DAG)。
    *   然後 `BatchOrchestrationService` 起始工作流程：
        *   可能會呼叫 `AFPGenerationService` 並帶有參數以重新擷取資料或使用新資料，並重新產生 AFP。
        *   後續步驟 (轉換、電子郵件) 將按照批次處理設計進行，可能涉及服務之間的訊息佇列。

專用的 `StatusQueryService` 可能有助於整合來自各種服務的狀態資訊，如果狀態追蹤變得複雜且分散。最初，狀態查詢可能會直接傳送到對該特定狀態最具權威性的服務。
