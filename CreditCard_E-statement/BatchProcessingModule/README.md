# Batch Processing Module (Orchestrator)

This module orchestrates the e-statement generation process by processing a batch of items from a JSON input file. For each item, it sequentially invokes other (demonstrator) modules:
1.  **AFP Generation Module:** Creates an item-specific XML from batch data and calls `AfpGenerator` to produce an AFP file.
2.  **AFP Conversion Module:** Calls `AfpConversionService` to (simulate) convert the generated AFP to PDF using a dummy script.
3.  **Email Assembly Module:** Calls `EmailAssembler` to create an email (MimeMessage) with the PDF as an attachment.
4.  **Email Sender Module:** Calls `EmailSender` to (simulate) send the assembled email to a test SMTP server.

## Prerequisites
- Java JDK 11 or higher (due to dependencies like GreenMail in EmailSenderModule, and to maintain consistency as EmailSenderModule was set to 11)
- Apache Maven
- A dummy SMTP server (e.g., Python's `smtpd`) running on `localhost:1025` for the email sending step.

## Build
```bash
mvn clean package
```
This will compile the module. Note that for the direct instantiation of classes from other modules to work when running the `main` method (especially from the command line), you would need to ensure those other modules' classes are on the classpath. IDEs often handle this if modules are part of the same project.

## Input File Format
The input is a JSON array of objects, specified in `src/main/resources/batch_input.json`. Each object should contain at least `customerId`, `name`, `statementDate`, and `email`.

## Running the `main` method for Demonstration

The `BatchProcessor.java` class contains a `main` method that drives the orchestration.

**Steps:**

1.  **Ensure Dependent Modules are "Built" (their classes available):**
    If running from an IDE that compiles all modules in the `CreditCard_E-statement` project, this might be handled automatically.
    If running from command line, you'd need to build each module (`AFPGenerationModule`, `AFP_Conversion_Module_Design`, `EmailAssemblyModule`, `EmailSenderModule`) first using `mvn clean package` in their respective directories to create their JARs or have their classes in their `target/classes` directories. Then, you'd run the `BatchProcessor.main()` with a complex classpath.
    **For simplicity, running from an IDE that manages the classpath across these modules is highly recommended for this demonstrator.**

2.  **Start a Dummy SMTP Server:**
    Open a terminal and run:
    ```bash
    python -m smtpd -n -c DebuggingServer localhost:1025
    ```

3.  **Run the `BatchProcessor.main()` method:**
    *   **From an IDE:** Right-click on `BatchProcessor.java` in this module and select "Run 'BatchProcessor.main()'".
    *   **From Command Line (Complex - requires manual classpath setup):**
        ```bash
        # Example assuming all modules have been built and target/classes exist
        # You are in CreditCard_E-statement/BatchProcessingModule
        java -cp "target/classes:\
        ../AFPGenerationModule/target/classes:\
        ../AFP_Conversion_Module_Design/target/classes:\
        ../EmailAssemblyModule/target/classes:\
        ../EmailSenderModule/target/classes:\
        $HOME/.m2/repository/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar:\
        $HOME/.m2/repository/org/apache/xmlgraphics/fop/2.8/fop-2.8.jar: \
        # ... (add ALL other numerous dependencies for FOP, Jakarta Mail, dnsjava, etc.) ...
        $HOME/.m2/repository/jakarta/activation/jakarta.activation-api/2.0.1/jakarta.activation-api-2.0.1.jar" \
        com.example.batchprocessor.BatchProcessor src/main/resources/batch_input.json
        ```
        *(The command line execution is shown for illustration of complexity; IDE execution is preferred for this setup).*

4.  **Observe Output:**
    *   The Java application will output logs for each step of the process for each batch item.
    *   Dummy AFP, PDF, and EML files will be created in `CreditCard_E-statement/BatchProcessingModule/output_batch/` subdirectories.
    *   The Python dummy SMTP server will print the full content of the emails it receives.

## Testing
Unit tests mock the interactions with the other modules.
```bash
mvn test
```
This will execute tests like `BatchProcessorTest`, which verifies the orchestration logic (sequence of calls, parameter passing) without actually running the full external modules.

## Notes
- The `BatchProcessor` uses direct instantiation of classes from other modules (e.g., `new AfpGenerator()`). This is a simplification for this demonstration. In a production microservices architecture, these calls would typically be REST API calls or messages via a queue.
- Error handling is basic; if a step fails for an item, it's logged, and the processor moves to the next item.
- Paths to resources in other modules (like `template.fo`) are relative. This assumes a specific directory structure when running.
- The `pom.xml` for this module does *not* explicitly list other local modules as Maven dependencies. This means an IDE is relied upon to resolve these cross-module class references, or a complex manual classpath is needed for command-line execution. For a true multi-module Maven project, inter-module dependencies would be declared in the `pom.xml` files.

---
## 正體中文 (Traditional Chinese)

# 批次處理模組 (協調器)

本模組透過處理 JSON 輸入檔案中的一批項目來協調電子帳單產生程序。對於每個項目，它會依序叫用其他 (示範) 模組：
1.  **AFP 產生模組：** 從批次資料建立項目特定的 XML，並呼叫 `AfpGenerator` 以產生 AFP 檔案。
2.  **AFP 轉換模組：** 呼叫 `AfpConversionService` 以使用虛設指令碼 (模擬) 將產生的 AFP 轉換為 PDF。
3.  **電子郵件組合模組：** 呼叫 `EmailAssembler` 以建立包含 PDF 附件的電子郵件 (MimeMessage)。
4.  **電子郵件傳送模組：** 呼叫 `EmailSender` 以 (模擬) 將組合的電子郵件傳送到測試 SMTP 伺服器。

## 先決條件
- Java JDK 11 或更高版本 (由於 EmailSenderModule 中的 GreenMail 等相依性，且為了保持一致性，因為 EmailSenderModule 設定為 11)
- Apache Maven
- 一個虛設的 SMTP 伺服器 (例如 Python 的 `smtpd`) 在 `localhost:1025` 上執行，用於電子郵件傳送步驟。

## 建置
```bash
mvn clean package
```
這將編譯模組。請注意，為了讓執行 `main` 方法時 (尤其是在命令列中) 其他模組類別的直接具現化能夠運作，您需要確保這些其他模組的類別位於類別路徑中。如果模組是同一個專案的一部分，IDE 通常會處理這個問題。

## 輸入檔案格式
輸入是 JSON 物件陣列，在 `src/main/resources/batch_input.json` 中指定。每個物件至少應包含 `customerId`、`name`、`statementDate` 和 `email`。

## 執行 `main` 方法進行示範

`BatchProcessor.java` 類別包含一個驅動協調的 `main` 方法。

**步驟：**

1.  **確保相依模組已「建置」(其類別可用)：**
    如果在編譯 `CreditCard_E-statement` 專案中所有模組的 IDE 中執行，這可能會自動處理。
    如果從命令列執行，您需要先在各自的目錄中使用 `mvn clean package` 建置每個模組 (`AFPGenerationModule`、`AFP_Conversion_Module_Design`、`EmailAssemblyModule`、`EmailSenderModule`) 以建立其 JAR 檔案或使其類別位於其 `target/classes` 目錄中。然後，您需要使用複雜的類別路徑執行 `BatchProcessor.main()`。
    **為簡單起見，強烈建議在此示範器中使用可管理這些模組間類別路徑的 IDE 執行。**

2.  **啟動虛設 SMTP 伺服器：**
    開啟一個終端機並執行：
    ```bash
    python -m smtpd -n -c DebuggingServer localhost:1025
    ```

3.  **執行 `BatchProcessor.main()` 方法：**
    *   **從 IDE：** 在此模組中右鍵按一下 `BatchProcessor.java` 並選取「執行 'BatchProcessor.main()'」。
    *   **從命令列 (複雜 - 需要手動設定類別路徑)：**
        ```bash
        # 假設所有模組都已建置且 target/classes 存在
        # 您位於 CreditCard_E-statement/BatchProcessingModule
        java -cp "target/classes:\
        ../AFPGenerationModule/target/classes:\
        ../AFP_Conversion_Module_Design/target/classes:\
        ../EmailAssemblyModule/target/classes:\
        ../EmailSenderModule/target/classes:\
        $HOME/.m2/repository/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar:\
        $HOME/.m2/repository/org/apache/xmlgraphics/fop/2.8/fop-2.8.jar: \
        # ... (為 FOP、Jakarta Mail、dnsjava 等新增所有其他眾多相依性) ...
        $HOME/.m2/repository/jakarta/activation/jakarta.activation-api/2.0.1/jakarta.activation-api-2.0.1.jar" \
        com.example.batchprocessor.BatchProcessor src/main/resources/batch_input.json
        ```
        *(命令列執行僅為說明複雜性；此設定建議使用 IDE 執行)。*

4.  **觀察輸出：**
    *   Java 應用程式將為每個批次項目的每個處理步驟輸出日誌。
    *   虛設的 AFP、PDF 和 EML 檔案將建立在 `CreditCard_E-statement/BatchProcessingModule/output_batch/` 子目錄中。
    *   Python 虛設 SMTP 伺服器將列印其收到的電子郵件的完整內容。

## 測試
單元測試會模擬與其他模組的互動。
```bash
mvn test
```
這將執行類似 `BatchProcessorTest` 的測試，該測試驗證協調邏輯 (呼叫順序、參數傳遞)，而無需實際執行完整的外部模組。

## 注意事項
- `BatchProcessor` 使用來自其他模組類別的直接具現化 (例如 `new AfpGenerator()`)。這是此示範的簡化。在生產微服務架構中，這些呼叫通常是 REST API 呼叫或透過佇列的訊息。
- 錯誤處理是基本的；如果某個項目的某個步驟失敗，則會記錄下來，處理器會移至下一個項目。
- 其他模組中資源 (如 `template.fo`) 的路徑是相對的。這假設執行時具有特定的目錄結構。
- 此模組的 `pom.xml` *未*明確列出其他本機模組作為 Maven 相依性。這表示依賴 IDE 解析這些跨模組類別參考，或者命令列執行需要複雜的手動類別路徑。對於真正的多模組 Maven 專案，模組間相依性會在 `pom.xml` 檔案中宣告。
