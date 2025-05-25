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
