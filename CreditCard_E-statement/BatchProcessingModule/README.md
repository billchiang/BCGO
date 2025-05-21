# Batch Processing Module (Simplified Simulation)

This module simulates batch processing of statements. It reads a JSON input file, and for each item,
it simulates AFP generation, PDF conversion, EML assembly, and email sending by creating dummy output files
and logging actions to the console.

## Prerequisites
- Java JDK 8 or higher
- Apache Maven

## Build
```bash
mvn clean package
```

## Input File Format
The input is a JSON array of objects, specified in `src/main/resources/batch_input.json`. Each object should contain at least `customerId`, `name`, `statementDate`, and `email`.

## Run
The `BatchProcessor` class has a `main` method.
```bash
# Ensure an 'output_batch' directory will be created by the application in the module's root
java -cp target/batch-processing-module-1.0-SNAPSHOT.jar com.example.batchprocessor.BatchProcessor src/main/resources/batch_input.json
# Or run without args to use default input path
java -cp target/batch-processing-module-1.0-SNAPSHOT.jar com.example.batchprocessor.BatchProcessor
```
This will create dummy .afp, .pdf, and .eml files in `output_batch/afp/`, `output_batch/pdf/`, and `output_batch/eml/` subdirectories respectively, and print a summary to the console.

**Note:** This is a simplified simulation. It does not integrate with the actual AFP Generation, Conversion, or Email Sender modules yet. That integration is a future step.
