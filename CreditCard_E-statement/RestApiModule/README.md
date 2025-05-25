# REST API Module (Scaffold)

This module provides a basic RESTful API for statement status and download, using Spring Boot.
It includes a **simulated token-based authentication and authorization** mechanism.

## Prerequisites
- Java JDK 8 or higher (current pom.xml uses Java 8 for Spring Boot 2.6.3)
- Apache Maven

## Build
```bash
mvn clean package
```

## Run
```bash
java -jar target/rest-api-module-0.0.1-SNAPSHOT.jar
```
The application will start on port 8080 by default.

## Simulated Authentication/Authorization

The API endpoints under `/api/v1/statements/**` are protected by a simulated `AuthInterceptor`.
-   It expects an `Authorization` header with a Bearer token (e.g., `Authorization: Bearer YOUR_TOKEN_HERE`).
-   **Valid Token Format (Dummy):** `VALID_TOKEN_{ROLE_OR_USER_INFO}_SCOPE_{PERMISSION1}_SCOPE_{PERMISSION2}...`
    -   The part after `VALID_TOKEN_` is checked for the presence of required "scope" strings.
    -   Example valid token for full access to current endpoints: `VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD`
-   **Recognized Permissions (Simulated Scopes):**
    -   `STATEMENT_READ`: Required for `GET /api/v1/statements/{id}/status`
    -   `STATEMENT_DOWNLOAD`: Required for `GET /api/v1/statements/{id}/download`

## Example API Calls

**Dummy File Setup (if not running BatchProcessingModule first):**
(Run these commands from the `CreditCard_E-statement/RestApiModule` directory if you are running the JAR from there)
```bash
mkdir -p ../BatchProcessingModule/output_batch/pdf
mkdir -p ../BatchProcessingModule/output_batch/afp
echo "dummy PDF content CUST001_20240101" > ../BatchProcessingModule/output_batch/pdf/CUST001_20240101.pdf
echo "dummy AFP content CUST002_20240102" > ../BatchProcessingModule/output_batch/afp/CUST002_20240102.afp
rm -f ../BatchProcessingModule/output_batch/pdf/CUST002_20240102.pdf # Ensure no PDF for AFP_Generated status test
```

**Tokens for Examples:**
-   `TOKEN_FULL_ACCESS="VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD"`
-   `TOKEN_READ_ONLY="VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ"`
-   `TOKEN_DOWNLOAD_ONLY="VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD"`
-   `TOKEN_NO_RELEVANT_SCOPES="VALID_TOKEN_GUEST_SCOPE_PROFILE_READ"`
-   `TOKEN_INVALID="INVALID_TOKEN_TEST"`

---
**1. Get Statement Status (`STATEMENT_READ` scope required)**

*   **Successful (Full Access Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 200 with status details.

*   **Successful (Read-Only Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 200 with status details.

*   **Forbidden (Download-Only Token - missing `STATEMENT_READ`):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 403 Forbidden.

*   **Forbidden (No Relevant Scopes Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_GUEST_SCOPE_PROFILE_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 403 Forbidden.

*   **Unauthorized (Missing Token):**
    ```bash
    curl http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 401 Unauthorized.

*   **Forbidden (Invalid Token):**
    ```bash
    curl -H "Authorization: Bearer INVALID_TOKEN_TEST" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    Expected: HTTP 403 Forbidden.

---
**2. Download Statement PDF (`STATEMENT_DOWNLOAD` scope required)**

*   **Successful (Full Access Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD" -o statement.pdf http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    Expected: HTTP 200, PDF file downloaded.

*   **Successful (Download-Only Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD" -o statement.pdf http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    Expected: HTTP 200, PDF file downloaded.

*   **Forbidden (Read-Only Token - missing `STATEMENT_DOWNLOAD`):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    Expected: HTTP 403 Forbidden.

*   **Forbidden (No Relevant Scopes Token):**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_GUEST_SCOPE_PROFILE_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    Expected: HTTP 403 Forbidden.

*   **Unauthorized (Missing Token):**
    ```bash
    curl http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    Expected: HTTP 401 Unauthorized.

---
## Notes
- The file paths in `StatementService.java` are relative (e.g., `../BatchProcessingModule/output_batch/...`). Ensure this structure is valid from where the `RestApiModule` JAR is run.
- Unit tests in `StatementApiControllerTest.java` mock the `StatementService` and now also test various authentication/authorization scenarios.
