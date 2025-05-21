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
