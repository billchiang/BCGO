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

---
## 正體中文 (Traditional Chinese)

# REST API 模組 (腳手架)

本模組使用 Spring Boot 提供一個基本的 RESTful API，用於帳單狀態和下載。
它包含一個**模擬的基於權杖的驗證和授權**機制。

## 先決條件
- Java JDK 8 或更高版本 (目前的 pom.xml 使用 Java 8 for Spring Boot 2.6.3)
- Apache Maven

## 建置
```bash
mvn clean package
```

## 執行
```bash
java -jar target/rest-api-module-0.0.1-SNAPSHOT.jar
```
應用程式預設會在連接埠 8080 上啟動。

## 模擬驗證/授權

`/api/v1/statements/**` 下的 API 端點受到模擬的 `AuthInterceptor` 保護。
-   它預期 `Authorization` 標頭中帶有 Bearer 權杖 (例如 `Authorization: Bearer YOUR_TOKEN_HERE`)。
-   **有效權杖格式 (虛設)：** `VALID_TOKEN_{ROLE_OR_USER_INFO}_SCOPE_{PERMISSION1}_SCOPE_{PERMISSION2}...`
    -   檢查 `VALID_TOKEN_` 之後的部分是否包含必要的「範圍」字串。
    -   範例：目前端點的完整存取權杖：`VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD`
-   **可辨識的權限 (模擬範圍)：**
    -   `STATEMENT_READ`：`GET /api/v1/statements/{id}/status` 所需
    -   `STATEMENT_DOWNLOAD`：`GET /api/v1/statements/{id}/download` 所需

## API 呼叫範例

**虛設檔案設定 (如果未先執行 BatchProcessingModule)：**
(如果您從 `CreditCard_E-statement/RestApiModule` 目錄執行 JAR，請從該目錄執行這些指令)
```bash
mkdir -p ../BatchProcessingModule/output_batch/pdf
mkdir -p ../BatchProcessingModule/output_batch/afp
echo "dummy PDF content CUST001_20240101" > ../BatchProcessingModule/output_batch/pdf/CUST001_20240101.pdf
echo "dummy AFP content CUST002_20240102" > ../BatchProcessingModule/output_batch/afp/CUST002_20240102.afp
rm -f ../BatchProcessingModule/output_batch/pdf/CUST002_20240102.pdf # 確保 AFP_Generated 狀態測試沒有 PDF
```

**範例權杖：**
-   `TOKEN_FULL_ACCESS="VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD"`
-   `TOKEN_READ_ONLY="VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ"`
-   `TOKEN_DOWNLOAD_ONLY="VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD"`
-   `TOKEN_NO_RELEVANT_SCOPES="VALID_TOKEN_GUEST_SCOPE_PROFILE_READ"`
-   `TOKEN_INVALID="INVALID_TOKEN_TEST"`

---
**1. 取得帳單狀態 (需要 `STATEMENT_READ` 範圍)**

*   **成功 (完整存取權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 200 並附帶狀態詳細資訊。

*   **成功 (唯讀權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 200 並附帶狀態詳細資訊。

*   **禁止 (僅下載權杖 - 缺少 `STATEMENT_READ`)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 403 Forbidden。

*   **禁止 (無相關範圍權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_GUEST_SCOPE_PROFILE_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 403 Forbidden。

*   **未經授權 (缺少權杖)：**
    ```bash
    curl http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 401 Unauthorized。

*   **禁止 (無效權杖)：**
    ```bash
    curl -H "Authorization: Bearer INVALID_TOKEN_TEST" http://localhost:8080/api/v1/statements/CUST001_20240101/status
    ```
    預期：HTTP 403 Forbidden。

---
**2. 下載帳單 PDF (需要 `STATEMENT_DOWNLOAD` 範圍)**

*   **成功 (完整存取權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD" -o statement.pdf http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    預期：HTTP 200，PDF 檔案已下載。

*   **成功 (僅下載權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD" -o statement.pdf http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    預期：HTTP 200，PDF 檔案已下載。

*   **禁止 (唯讀權杖 - 缺少 `STATEMENT_DOWNLOAD`)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    預期：HTTP 403 Forbidden。

*   **禁止 (無相關範圍權杖)：**
    ```bash
    curl -H "Authorization: Bearer VALID_TOKEN_GUEST_SCOPE_PROFILE_READ" http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    預期：HTTP 403 Forbidden。

*   **未經授權 (缺少權杖)：**
    ```bash
    curl http://localhost:8080/api/v1/statements/CUST001_20240101/download?format=pdf
    ```
    預期：HTTP 401 Unauthorized。

---
## 注意事項
- `StatementService.java` 中的檔案路徑是相對的 (例如 `../BatchProcessingModule/output_batch/...`)。確保此結構從執行 `RestApiModule` JAR 的位置開始是有效的。
- `StatementApiControllerTest.java` 中的單元測試模擬了 `StatementService`，現在也測試各種驗證/授權情境。
