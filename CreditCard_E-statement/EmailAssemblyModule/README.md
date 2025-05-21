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
