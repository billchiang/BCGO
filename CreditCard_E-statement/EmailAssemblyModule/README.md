# Email Assembly Module

This module assembles an email with HTML content and a PDF attachment using Jakarta Mail.

## Prerequisites
- Java JDK 8 or higher
- Apache Maven

## Build
To compile the module and package it into a JAR:
```bash
mvn clean package
```

## Run
The `EmailAssembler` class has a `main` method. You can run it from your IDE or after packaging.
It expects a PDF file at `../AFP_Conversion_Module_Design/output/statement.pdf` relative to this module's root, or it will create a dummy one for demonstration.

To run the example from the command line after building:
1. Ensure an `output` directory exists at the root of the `EmailAssemblyModule` directory. If not, create it:
   ```bash
   mkdir output
   ```
2. Run the main class:
   ```bash
   java -cp target/email-assembly-module-1.0-SNAPSHOT.jar com.example.emailassembly.EmailAssembler
   ```
This will generate `output/assembled_email.eml`. You can open this file with an email client like Outlook or Thunderbird to inspect its structure and content.

You can also run the `EmailAssembler.main()` method directly from your IDE.

## Testing
Unit tests can be run using Maven:
```bash
mvn test
```
This will execute tests like `EmailAssemblerTest`, which verifies the structure and content of the assembled MimeMessage.

---
## 正體中文 (Traditional Chinese)

# 電子郵件組合模組

本模組使用 Jakarta Mail 將 HTML 內容和 PDF 附件組合成一封電子郵件。

## 先決條件
- Java JDK 8 或更高版本
- Apache Maven

## 建置
編譯模組並將其打包到 JAR 檔案中：
```bash
mvn clean package
```

## 執行
`EmailAssembler` 類別有一個 `main` 方法。您可以從 IDE 執行它，或在打包後執行。
它預期在此模組根目錄的相對路徑 `../AFP_Conversion_Module_Design/output/statement.pdf` 找到一個 PDF 檔案，否則它將建立一個虛設檔案進行示範。

建置後從命令列執行範例：
1. 確保 `EmailAssemblyModule` 目錄的根目錄中存在 `output` 目錄。如果不存在，請建立它：
   ```bash
   mkdir output
   ```
2. 執行主類別：
   ```bash
   java -cp target/email-assembly-module-1.0-SNAPSHOT.jar com.example.emailassembly.EmailAssembler
   ```
這將產生 `output/assembled_email.eml`。您可以使用 Outlook 或 Thunderbird 等電子郵件用戶端開啟此檔案，以檢查其結構和內容。

您也可以直接從 IDE 執行 `EmailAssembler.main()` 方法。

## 測試
可以使用 Maven 執行單元測試：
```bash
mvn test
```
這將執行類似 `EmailAssemblerTest` 的測試，該測試驗證組合的 MimeMessage 的結構和內容。
