package com.example.batchprocessor;

import com.example.commonutils.exceptions.CustomApplicationException;
import com.example.commonutils.exceptions.PermanentException;
import com.example.commonutils.exceptions.TransientException;
import com.example.commonutils.logging.LoggerUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

// Assuming these classes are accessible (e.g., via classpath or by adding module dependencies if this were a real multi-module Maven project)
// For this task, we'll use direct instantiation as if they are on the classpath.
// This will require manual classpath setup if running outside an IDE that resolves this.
import com.example.afpgen.AfpGenerator;
import com.example.afpconversion.AfpConversionService;
import com.example.afpconversion.model.ConversionResult; 
import com.example.emailassembly.EmailAssembler;
import com.example.emailsender.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger; // SLF4J Logger
import java.io.FileOutputStream; // For saving .eml file

public class BatchProcessor {

    private static final Logger log = LoggerUtil.getLogger(BatchProcessor.class);
    public static final String BASE_OUTPUT_DIR = "output_batch"; // Relative to module root
    private static final String TEMP_XML_DIR = "temp_xml";

    private static final String AFP_GENERATION_MODULE_PATH = "../AFPGenerationModule/";
    // private static final String AFP_CONVERSION_MODULE_PATH = "../AFP_Conversion_Module_Design/"; // Not directly used for resources
    // private static final String EMAIL_ASSEMBLY_MODULE_PATH = "../EmailAssemblyModule/"; // Not directly used for resources

    private AfpGenerator afpGenerator;
    private AfpConversionService afpConversionService;
    private EmailAssembler emailAssembler;
    private EmailSender emailSender;

    public BatchProcessor() {
        String correlationId = LoggerUtil.getOrGenerateCorrelationId(null);
        try {
            this.afpGenerator = new AfpGenerator(); 
            this.afpConversionService = new AfpConversionService(); 
            this.emailAssembler = new EmailAssembler(); 
            this.emailSender = new EmailSender("batch-sender@example.com", "batch-processor.example.com");
            this.emailSender.setOverrideSmtpServer("localhost", 1025); 
            log.info("BatchProcessor initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize BatchProcessor components: {}", e.getMessage(), e);
            throw new PermanentException("BatchProcessor initialization failed", e);
        } finally {
            LoggerUtil.clearCorrelationId();
        }
    }
    
    // Constructor for testing with mocks
    public BatchProcessor(AfpGenerator afpGenerator, AfpConversionService afpConversionService, 
                          EmailAssembler emailAssembler, EmailSender emailSender) {
        this.afpGenerator = afpGenerator;
        this.afpConversionService = afpConversionService;
        this.emailAssembler = emailAssembler;
        this.emailSender = emailSender;
        if (this.emailSender != null) { // Null check for safety
            this.emailSender.setOverrideSmtpServer("localhost", 1025);
        }
    }


    private String generateItemSpecificXml(BatchItem item, Path tempXmlDir, String correlationId) throws IOException {
        // Using existing correlationId
        LoggerUtil.setCorrelationId(correlationId);
        log.debug("Generating XML for customerId: {}", item.customerId);
        String xmlContent = String.format(
            "<statementData>\n" +
            "  <accountDetails>\n" +
            "    <bankName>Global Trust Bank (Batch)</bankName>\n" +
            "    <accountNumber>%s</accountNumber>\n" + 
            "  </accountDetails>\n" +
            "  <statementDetails>\n" +
            "    <statementDate>%s</statementDate>\n" +
            "    <paymentDueDate>2024-08-20</paymentDueDate>\n" + 
            "    <totalAmountDue>USD 123.45</totalAmountDue>\n" +   
            "  </statementDetails>\n" +
            "  <customer>\n" +
            "    <name>%s</name>\n" +
            "    <address>\n" +
            "      <street>123 Batch St</street>\n" + 
            "      <city>Processville</city>\n" +     
            "      <state>Workflow</state>\n" +        
            "      <zipCode>98765</zipCode>\n" +       
            "    </address>\n" +
            "  </customer>\n" +
            "  <banner>\n" +
            "    <imageUrl>http://example.com/images/batch_banner.png</imageUrl>\n" + 
            "    <altText>Batch Processed Statement</altText>\n" +                     
            "  </banner>\n" +
            "  <summary>\n" +
            "    <previousBalance>USD 50.00</previousBalance>\n" +   
            "    <paymentsReceived>USD 20.00</paymentsReceived>\n" + 
            "    <newCharges>USD 93.45</newCharges>\n" +           
            "    <newBalance>USD 123.45</newBalance>\n" +          
            "  </summary>\n" +
            "  <transactions>\n" +
            "    <transaction>\n" +
            "      <date>2024-07-15</date>\n" +
            "      <description>Batch Item Purchase 1</description>\n" + 
            "      <amount>USD 50.00</amount>\n" +                      
            "    </transaction>\n" +
            "    <transaction>\n" +
            "      <date>2024-07-20</date>\n" +
            "      <description>Batch Item Service Fee</description>\n" + 
            "      <amount>USD 43.45</amount>\n" +                       
            "    </transaction>\n" +
            "  </transactions>\n" +
            "  <notices>\n" +
            "    <noticeText>This is a batch generated statement. Please review details.</noticeText>\n" + 
            "  </notices>\n" +
            "  <paymentSlip>\n" +
            "    <barcodeData>%sDUE20240820AMT12345</barcodeData>\n" + 
            "  </paymentSlip>\n" +
            "  <termsAndConditions>\n" +
            "    <footerText>Batch terms and conditions apply. Visit example.com/batch-terms.</footerText>\n" + 
            "  </termsAndConditions>\n" +
            "</statementData>",
            item.customerId, item.statementDate, item.name, item.customerId
        );
        
        Path xmlFilePath = tempXmlDir.resolve(item.customerId + "_" + item.statementDate.replace("-", "") + "_" + UUID.randomUUID().toString() + ".xml");
        try (FileWriter writer = new FileWriter(xmlFilePath.toFile())) {
            writer.write(xmlContent);
        }
        log.debug("Generated item-specific XML: {}", xmlFilePath);
        return xmlFilePath.toString();
    }

    public void processBatch(String batchInputJsonFilePath) throws IOException {
        String batchCorrelationId = LoggerUtil.getOrGenerateCorrelationId(null); // For the overall batch
        log.info("Starting batch processing for input file: {}", batchInputJsonFilePath);

        Gson gson = new Gson();
        Type batchItemListType = new TypeToken<List<BatchItem>>() {}.getType();
        
        Path baseOutputDirPath = Paths.get(BASE_OUTPUT_DIR);
        Files.createDirectories(baseOutputDirPath.resolve("afp"));
        Files.createDirectories(baseOutputDirPath.resolve("pdf"));
        Files.createDirectories(baseOutputDirPath.resolve("eml"));
        Path tempXmlDir = baseOutputDirPath.resolve(TEMP_XML_DIR);
        Files.createDirectories(tempXmlDir);

        List<BatchItem> items;
        try (FileReader reader = new FileReader(batchInputJsonFilePath)) {
            items = gson.fromJson(reader, batchItemListType);
        } catch (IOException e) {
            log.error("Failed to read batch input file {}: {}", batchInputJsonFilePath, e.getMessage(), e);
            throw new PermanentException("Failed to read batch input file: " + batchInputJsonFilePath, e);
        }

        int totalItems = items.size();
        int successCount = 0;
        int failureCount = 0;
        log.info("Found {} items in batch file.", totalItems);

        String afpTemplatePath = AFP_GENERATION_MODULE_PATH + "src/main/resources/template.fo";

        for (BatchItem item : items) {
            String itemCorrelationId = LoggerUtil.generateCorrelationId(); // Unique ID for this item's processing
            LoggerUtil.setCorrelationId(itemCorrelationId); // Set for current item
            log.info("Processing item for customerId: {}, name: {}", item.customerId, item.name);
            
            String baseFileName = item.customerId + "_" + item.statementDate.replace("-", "");
            String itemSpecificXmlPath = null;
            String generatedAfpPath = baseOutputDirPath.resolve("afp/" + baseFileName + ".afp").toString();
            String convertedPdfPath = baseOutputDirPath.resolve("pdf/" + baseFileName + ".pdf").toString();
            String assembledEmlPath = baseOutputDirPath.resolve("eml/" + baseFileName + ".eml").toString();
            boolean itemFailed = false;

            try {
                // Step a: Generate Item-Specific XML & Invoke AFP Generation
                log.info("Step 1: Generating AFP for customerId: {}", item.customerId);
                itemSpecificXmlPath = generateItemSpecificXml(item, tempXmlDir, itemCorrelationId); // Pass correlationId
                afpGenerator.generateAfp(afpTemplatePath, itemSpecificXmlPath, generatedAfpPath); // This method now uses its own logging & exceptions
                log.info("AFP generated successfully: {}", generatedAfpPath);

                // Step b: Invoke AFP to PDF Conversion
                log.info("Step 2: Converting AFP to PDF for customerId: {}", item.customerId);
                ConversionResult conversionResult = afpConversionService.convertToPdf(generatedAfpPath, convertedPdfPath);
                if (!conversionResult.isSuccess()) {
                    throw new PermanentException("AFP to PDF conversion failed for " + generatedAfpPath + ": " + conversionResult.getErrorMessage());
                }
                log.info("PDF converted successfully: {}", convertedPdfPath);

                // Step c: Invoke Email Assembly
                log.info("Step 3: Assembling Email for customerId: {}", item.customerId);
                MimeMessage mimeMessage = emailAssembler.assembleEmail(
                    item.email,
                    "Your Statement for " + item.statementDate,
                    item.name,
                    item.statementDate,
                    convertedPdfPath
                );
                try (FileOutputStream fos = new FileOutputStream(assembledEmlPath)) {
                    mimeMessage.writeTo(fos);
                }
                log.info("Email assembled successfully: {}", assembledEmlPath);
                
                // Step d: Invoke Email Sending
                log.info("Step 4: Sending Email for customerId: {}", item.customerId);
                boolean emailSent = emailSender.sendEmail(item.email, mimeMessage);
                if (!emailSent) {
                    // EmailSender's sendEmail logs details, this can be a more specific exception
                    throw new TransientException("Email sending failed for " + item.email + " (check EmailSender logs for details)");
                }
                log.info("Email sent successfully to: {}", item.email);
                successCount++;

            } catch (CustomApplicationException e) { // Catch our specific custom exceptions
                log.error("FAILED processing item for customerId {}: ({} - {})", item.customerId, e.getClass().getSimpleName(), e.getMessage(), e);
                itemFailed = true;
                failureCount++;
            } catch (Exception e) { // Catch any other unexpected exceptions
                log.error("FAILED processing item for customerId {} with unexpected error: {}", item.customerId, e.getMessage(), e);
                itemFailed = true;
                failureCount++;
            } finally {
                if (itemSpecificXmlPath != null) {
                    try {
                        Files.deleteIfExists(Paths.get(itemSpecificXmlPath));
                    } catch (IOException ex) {
                        log.warn("Error deleting temp XML file {}: {}", itemSpecificXmlPath, ex.getMessage());
                    }
                }
                log.info("Finished processing for customerId: {}. Status: {}", item.customerId, (itemFailed ? "FAILED" : "SUCCESS"));
                LoggerUtil.clearCorrelationId(); // Clear item-specific correlation ID
            }
        }
        
        LoggerUtil.setCorrelationId(batchCorrelationId); // Restore batch-level correlation ID for summary
        
        try { // Clean up the whole temp_xml directory
            if (Files.exists(tempXmlDir)) { // Check if directory exists before walking
                Files.walk(tempXmlDir)
                     .sorted(java.util.Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
                Files.deleteIfExists(tempXmlDir); 
            }
        } catch (IOException e) {
            log.warn("Warning: Could not fully clean up temporary XML directory: {}", tempXmlDir.toString(), e);
        }

        log.info("--- Batch Processing Summary ---");
        log.info("Total items processed: {}", totalItems);
        log.info("Successful items: {}", successCount);
        log.info("Failed items: {}", failureCount);
        log.info("------------------------------");
        log.info("Output files are in: {}", baseOutputDirPath.toAbsolutePath());
        LoggerUtil.clearCorrelationId(); // Clear batch-level correlation ID
    }

    public static void main(String[] args) {
        String mainCorrelationId = LoggerUtil.generateCorrelationId();
        LoggerUtil.setCorrelationId(mainCorrelationId);
        final Logger mainLog = LoggerUtil.getLogger(BatchProcessor.class);

        try {
            mainLog.info("BatchProcessor main method started.");
            BatchProcessor processor = new BatchProcessor();
            LoggerUtil.setCorrelationId(mainCorrelationId); // Ensure main's correlation ID is set after processor init
            
            String inputFile = "src/main/resources/batch_input.json"; 
            if (args.length > 0) {
                inputFile = args[0];
            }
            mainLog.info("Processing batch from input file: {}", inputFile);
            mainLog.info("Ensure dummy SMTP server is running on localhost:1025 for email sending step.");
            processor.processBatch(inputFile);
        } catch (CustomApplicationException e) {
             mainLog.error("Batch processing failed due to application error: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        } catch (Exception e) {
            mainLog.error("Critical error during batch processing: {}", e.getMessage(), e);
        } finally {
            LoggerUtil.clearCorrelationId();
            mainLog.info("BatchProcessor main method finished.");
        }
    }
}
