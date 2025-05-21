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
