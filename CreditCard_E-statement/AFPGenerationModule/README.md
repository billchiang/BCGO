# AFP Generation Module

This module generates AFP files from XSL-FO templates (which can include XSLT instructions for data merging) and XML data using Apache FOP.

## Prerequisites
- Java JDK 8 or higher
- Apache Maven

## Build
To compile the module and package it into a JAR:
```bash
mvn clean package
```

## Run
The `AfpGenerator` class contains a `main` method that demonstrates its usage. It expects `template.fo` and `data.xml` to be in `src/main/resources/` and will create an `output/` directory in the module's root for the generated AFP file.

To run the example from the command line after building:
1. Ensure an `output` directory exists at the root of the `AFPGenerationModule` directory. If not, create it:
   ```bash
   mkdir output
   ```
2. Run the main class:
   ```bash
   java -cp target/afp-generation-module-1.0-SNAPSHOT.jar com.example.afpgen.AfpGenerator
   ```
This will generate `output/statement.afp`.

You can also run the `AfpGenerator.main()` method directly from your IDE.

## Testing
Unit tests can be run using Maven:
```bash
mvn test
```
This will execute tests like `AfpGeneratorTest`, which uses a temporary directory for test files.

---
## 正體中文 (Traditional Chinese)

# AFP 產生模組

本模組使用 Apache FOP 從 XSL-FO 範本（可包含用於資料合併的 XSLT 指令）和 XML 資料產生 AFP 檔案。

## 先決條件
- Java JDK 8 或更高版本
- Apache Maven

## 建置
編譯模組並將其打包到 JAR 檔案中：
```bash
mvn clean package
```

## 執行
`AfpGenerator` 類別包含一個 `main` 方法，示範其用法。它預期 `template.fo` 和 `data.xml` 位於 `src/main/resources/` 中，並將在模組的根目錄中建立一個 `output/` 目錄，用於存放產生的 AFP 檔案。

建置後從命令列執行範例：
1. 確保 `AFPGenerationModule` 目錄的根目錄中存在 `output` 目錄。如果不存在，請建立它：
   ```bash
   mkdir output
   ```
2. 執行主類別：
   ```bash
   java -cp target/afp-generation-module-1.0-SNAPSHOT.jar com.example.afpgen.AfpGenerator
   ```
這將產生 `output/statement.afp`。

您也可以直接從 IDE 執行 `AfpGenerator.main()` 方法。

## 測試
可以使用 Maven 執行單元測試：
```bash
mvn test
```
這將執行類似 `AfpGeneratorTest` 的測試，該測試使用暫存目錄來存放測試檔案。
