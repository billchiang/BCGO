# Custom SMTP Email Sender Module

This module provides a basic implementation of an SMTP client capable of sending emails by performing MX lookups and communicating directly with recipient mail servers. It can also be configured to send via a specific relay server for testing or operational purposes.

## Prerequisites
- Java JDK 11 or higher (due to GreenMail 2.0.0 and potentially other modern library features)
- Apache Maven

## Build
To compile the module and package it into a JAR:
```bash
mvn clean package
```

## Running the `main` method for Demonstration

The `EmailSender.java` class contains a `main` method that demonstrates its usage. By default, this `main` method is configured to send an email to `recipient@localhost` via an SMTP server running on `localhost:1025`.

**Steps for Manual Testing with a Dummy SMTP Server:**

1.  **Start a Dummy SMTP Server:**
    You can use Python's built-in `smtpd` module for a simple debugging server that prints email content to the console. Open a terminal and run:
    ```bash
    python -m smtpd -n -c DebuggingServer localhost:1025
    ```
    This server will listen on `localhost` port `1025`.

2.  **Run the `EmailSender.main()` method:**
    *   **From an IDE:** Right-click on `EmailSender.java` and select "Run 'EmailSender.main()'".
    *   **From Command Line (after building with Maven):**
        ```bash
        # Ensure you are in the 'CreditCard_E-statement/EmailSenderModule' directory
        java -cp target/email-sender-module-1.0-SNAPSHOT.jar com.example.emailsender.EmailSender
        ```

3.  **Observe Output:**
    *   The Java application will output the SMTP dialogue (CLIENT and SERVER lines) to its console.
    *   The Python dummy SMTP server will print the full content of the email it receives to its console.

## Testing with GreenMail (Unit Tests)

Unit tests are implemented using GreenMail, an in-memory email server for testing.
To run the tests:
```bash
mvn test
```
The `EmailSenderTest.java` class starts a GreenMail SMTP server on a dynamic port (currently configured for 3025) and verifies that the `EmailSender` can successfully send a message to it. The test then checks if GreenMail received the message and inspects its content.

## Key Features and Configuration

*   **MX Lookup:** Uses `dnsjava` to find recipient mail servers. Falls back to A record lookup if no MX records are found.
*   **Direct SMTP Communication:** Implements basic SMTP commands (`HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`).
*   **MimeMessage Support:** Sends emails constructed as `jakarta.mail.internet.MimeMessage` objects.
*   **Configurable `mailFrom` and `heloDomain`:** Can be set in the `EmailSender` constructor.
*   **SMTP Server Override:** The `setOverrideSmtpServer(String host, int port)` method allows bypassing MX lookup to send all emails through a specified server, which is useful for testing (as used in `main` and unit tests) or routing through a smart host.

## Notes on the Implementation

*   **Error Handling:** Basic error handling is in place. More sophisticated retry logic for transient network errors or temporary SMTP error codes (4xx) would be needed for a production-grade system.
*   **STARTTLS:** The current implementation does not include `STARTTLS` for encrypting the SMTP session. This is a critical feature for secure email transmission over the public internet and should be added for production use.
*   **Character Encoding:** PrintWriter and BufferedReader are now initialized with UTF-8.
*   **DATA Termination:** The `\r\n.\r\n` sequence for terminating the DATA command is sent directly via `OutputStream` for reliability.

---
## 正體中文 (Traditional Chinese)

# 自訂 SMTP 電子郵件傳送模組

本模組提供 SMTP 用戶端的基本實作，能夠透過執行 MX 查閱並直接與收件人郵件伺服器通訊來傳送電子郵件。它也可以設定為透過特定中繼伺服器傳送，以供測試或營運之用。

## 先決條件
- Java JDK 11 或更高版本 (因 GreenMail 2.0.0 及其他現代函式庫功能)
- Apache Maven

## 建置
編譯模組並將其打包到 JAR 檔案中：
```bash
mvn clean package
```

## 執行 `main` 方法進行示範

`EmailSender.java` 類別包含一個 `main` 方法，用於示範其用法。預設情況下，此 `main` 方法設定為透過在 `localhost:1025` 上執行的 SMTP 伺服器將電子郵件傳送到 `recipient@localhost`。

**使用虛設 SMTP 伺服器進行手動測試的步驟：**

1.  **啟動虛設 SMTP 伺服器：**
    您可以使用 Python 內建的 `smtpd` 模組作為簡單的偵錯伺服器，它會將電子郵件內容列印到主控台。開啟一個終端機並執行：
    ```bash
    python -m smtpd -n -c DebuggingServer localhost:1025
    ```
    此伺服器將在 `localhost` 的 `1025` 連接埠上偵聽。

2.  **執行 `EmailSender.main()` 方法：**
    *   **從 IDE：** 右鍵按一下 `EmailSender.java` 並選取「執行 'EmailSender.main()'」。
    *   **從命令列 (使用 Maven 建置後)：**
        ```bash
        # 確保您位於 'CreditCard_E-statement/EmailSenderModule' 目錄中
        java -cp target/email-sender-module-1.0-SNAPSHOT.jar com.example.emailsender.EmailSender
        ```

3.  **觀察輸出：**
    *   Java 應用程式會將 SMTP 對話 (CLIENT 和 SERVER 行) 輸出到其主控台。
    *   Python 虛設 SMTP 伺服器會將其收到的電子郵件的完整內容列印到其主控台。

## 使用 GreenMail 進行測試 (單元測試)

單元測試是使用 GreenMail (一個用於測試的記憶體內電子郵件伺服器) 實作的。
若要執行測試：
```bash
mvn test
```
`EmailSenderTest.java` 類別會在動態連接埠 (目前設定為 3025) 上啟動 GreenMail SMTP 伺服器，並驗證 `EmailSender` 是否可以成功向其傳送訊息。然後，測試會檢查 GreenMail 是否收到訊息並檢查其內容。

## 主要功能與組態

*   **MX 查閱：** 使用 `dnsjava` 尋找收件人郵件伺服器。如果找不到 MX 記錄，則後備到 A 記錄查閱。
*   **直接 SMTP 通訊：** 實作基本的 SMTP 指令 (`HELO`、`MAIL FROM`、`RCPT TO`、`DATA`、`QUIT`)。
*   **MimeMessage 支援：** 傳送建構為 `jakarta.mail.internet.MimeMessage` 物件的電子郵件。
*   **可設定的 `mailFrom` 和 `heloDomain`：** 可在 `EmailSender` 建構函式中設定。
*   **SMTP 伺服器覆寫：** `setOverrideSmtpServer(String host, int port)` 方法允許繞過 MX 查閱，將所有電子郵件透過指定的伺服器傳送，這對於測試 (如 `main` 和單元測試中所用) 或透過智慧主機路由非常有用。

## 實作注意事項

*   **錯誤處理：** 已實作基本的錯誤處理。對於生產級系統，需要更複雜的重試邏輯來處理暫時性網路錯誤或暫時性 SMTP 錯誤碼 (4xx)。
*   **STARTTLS：** 目前的實作不包含用於加密 SMTP 工作階段的 `STARTTLS`。這是透過公用網際網路安全傳輸電子郵件的關鍵功能，應為生產用途新增。
*   **字元編碼：** PrintWriter 和 BufferedReader 現在使用 UTF-8 進行初始化。
*   **DATA 終止：** 為確保可靠性，用於終止 DATA 指令的 `\r\n.\r\n` 序列會直接透過 `OutputStream` 傳送。
