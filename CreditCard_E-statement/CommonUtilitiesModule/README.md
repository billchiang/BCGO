# Common Utilities Module

This module provides common utility classes for use across other modules in the CreditCard E-statement system.

## Features

### 1. Custom Exceptions
Located in `com.example.commonutils.exceptions`:
-   `CustomApplicationException.java`: Base runtime exception for the application.
-   `TransientException.java`: Extends `CustomApplicationException`. Intended for errors that might be resolved by a retry (e.g., temporary network issues, timeouts).
-   `PermanentException.java`: Extends `CustomApplicationException`. Intended for errors that are unlikely to be resolved by a simple retry (e.g., invalid data, configuration errors, file not found).

### 2. Logging Utilities
Located in `com.example.commonutils.logging`:
-   `LoggerUtil.java`: A utility class for SLF4J logging.
    -   Provides a static method `getLogger(Class<?> clazz)` to obtain an SLF4J logger instance.
    -   Includes methods for managing a `correlationId` in the Mapped Diagnostic Context (MDC):
        -   `setCorrelationId(String correlationId)`: Sets or generates a new correlation ID in MDC.
        -   `getOrGenerateCorrelationId(String correlationId)`: Returns the given ID if valid, or generates/sets/returns a new one.
        -   `generateCorrelationId()`: Generates a new UUID-based correlation ID.
        -   `clearCorrelationId()`: Clears the correlation ID from MDC.

### 3. Logback Configuration
-   `src/main/resources/logback.xml`: Provides a default Logback configuration.
    -   Configures a console appender (`STDOUT`).
    -   Includes `correlationId` from MDC in the log pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [%X{correlationId}] - %msg%n`.
    -   Sets root log level to `INFO`.
    -   Sets `com.example` package (and sub-packages) log level to `DEBUG`.

## Usage

To use this module in another Maven module:
1.  **Build and Install `CommonUtilitiesModule`:**
    Navigate to `CreditCard_E-statement/CommonUtilitiesModule` and run:
    ```bash
    mvn clean install
    ```
    This will build the JAR and install it into your local Maven repository.

2.  **Add as Dependency:**
    In the `pom.xml` of the dependent module (e.g., `AFPGenerationModule`), add this dependency:
    ```xml
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>common-utilities</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    ```

## Building
To compile the module and package it into a JAR:
```bash
mvn clean package
```

## Testing
Unit tests are provided for the exception classes and `LoggerUtil`.
```bash
mvn test
```

---
## 正體中文 (Traditional Chinese)

# 通用工具程式模組

本模組提供通用工具程式類別，供信用卡電子帳單系統中的其他模組使用。

## 功能

### 1. 自訂例外
位於 `com.example.commonutils.exceptions`：
-   `CustomApplicationException.java`：應用程式的基礎執行階段例外。
-   `TransientException.java`：擴展 `CustomApplicationException`。適用於可能透過重試解決的錯誤 (例如，暫時的網路問題、逾時)。
-   `PermanentException.java`：擴展 `CustomApplicationException`。適用於不太可能透過簡單重試解決的錯誤 (例如，無效資料、組態錯誤、找不到檔案)。

### 2. 日誌記錄工具程式
位於 `com.example.commonutils.logging`：
-   `LoggerUtil.java`：一個用於 SLF4J 日誌記錄的工具程式類別。
    -   提供靜態方法 `getLogger(Class<?> clazz)` 以取得 SLF4J 日誌記錄器執行個體。
    -   包含在對應診斷內容 (MDC) 中管理 `correlationId` 的方法：
        -   `setCorrelationId(String correlationId)`：在 MDC 中設定或產生新的關聯 ID。
        -   `getOrGenerateCorrelationId(String correlationId)`：如果提供的 ID 有效則傳回該 ID，否則產生/設定/傳回新的 ID。
        -   `generateCorrelationId()`：產生新的基於 UUID 的關聯 ID。
        -   `clearCorrelationId()`：從 MDC 中清除關聯 ID。

### 3. Logback 組態
-   `src/main/resources/logback.xml`：提供預設的 Logback 組態。
    -   設定主控台附加程式 (`STDOUT`)。
    -   在日誌模式中包含來自 MDC 的 `correlationId`：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [%X{correlationId}] - %msg%n`。
    -   將根日誌層級設定為 `INFO`。
    -   將 `com.example` 套件 (及子套件) 日誌層級設定為 `DEBUG`。

## 使用方式

在另一個 Maven 模組中使用此模組：
1.  **建置並安裝 `CommonUtilitiesModule`：**
    導覽至 `CreditCard_E-statement/CommonUtilitiesModule` 並執行：
    ```bash
    mvn clean install
    ```
    這將建置 JAR 檔案並將其安裝到您的本機 Maven 儲存庫。

2.  **新增為相依性：**
    在相依模組 (例如 `AFPGenerationModule`) 的 `pom.xml` 中，新增此相依性：
    ```xml
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>common-utilities</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    ```

## 建置
編譯模組並將其打包到 JAR 檔案中：
```bash
mvn clean package
```

## 測試
為例外類別和 `LoggerUtil` 提供了單元測試。
```bash
mvn test
```
