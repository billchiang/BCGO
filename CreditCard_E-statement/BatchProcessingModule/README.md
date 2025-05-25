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
