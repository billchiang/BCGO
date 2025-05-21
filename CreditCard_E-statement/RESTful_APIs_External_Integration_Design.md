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

I will now submit the report for this subtask.
