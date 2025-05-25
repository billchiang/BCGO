package com.example.afpgen;

import com.example.commonutils.exceptions.PermanentException;
import com.example.commonutils.exceptions.TransientException;
import com.example.commonutils.logging.LoggerUtil;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.slf4j.Logger;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

public class AfpGenerator {

    private static final Logger log = LoggerUtil.getLogger(AfpGenerator.class);
    private FopFactory fopFactory;

    public AfpGenerator() {
        String correlationId = LoggerUtil.getOrGenerateCorrelationId(null); // Ensure correlationId is set for constructor logging
        try {
            URI baseUri = new File(".").toURI(); 
            fopFactory = FopFactory.newInstance(baseUri);
            log.info("FopFactory initialized successfully with base URI: {}", baseUri);
        } catch (Exception e) { // Catch broader exceptions during FOP factory init
            log.error("Failed to initialize FopFactory: {}", e.getMessage(), e);
            // This is a critical failure for the AfpGenerator, so rethrow as a permanent exception
            throw new PermanentException("Failed to initialize FopFactory: " + e.getMessage(), e);
        } finally {
            LoggerUtil.clearCorrelationId(); // Clear if only set for constructor scope
        }
    }

    public void generateAfp(String foFilePath, String xmlFilePath, String outputAfpPath) {
        // Use existing correlationId if set, otherwise generate a new one for this operation
        String correlationId = LoggerUtil.getOrGenerateCorrelationId(null); 

        OutputStream out = null;
        log.info("Starting AFP generation. FO: {}, XML: {}, Output: {}", foFilePath, xmlFilePath, outputAfpPath);

        try {
            File outputFile = new File(outputAfpPath);
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                if (!outputFile.getParentFile().mkdirs()) {
                    // If mkdirs fails, it's a permanent issue for this output path
                    throw new PermanentException("Failed to create output directory: " + outputFile.getParentFile().getAbsolutePath());
                }
            }
            out = new FileOutputStream(outputFile);
            
            Fop fop = fopFactory.newFop(MimeConstants.MIME_AFP, out);

            TransformerFactory factory = TransformerFactory.newInstance();
            
            File xsltFile = new File(foFilePath);
            if (!xsltFile.exists()) {
                throw new PermanentException("XSL-FO template file not found: " + foFilePath);
            }
            File xmlFile = new File(xmlFilePath);
            if (!xmlFile.exists()) {
                throw new PermanentException("XML data file not found: " + xmlFilePath);
            }

            StreamSource xsltSource = new StreamSource(xsltFile);
            StreamSource xmlSource = new StreamSource(xmlFile);

            Transformer transformer = factory.newTransformer(xsltSource);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(xmlSource, res);
            log.info("AFP generated successfully: {}", outputAfpPath);

        } catch (FOPException e) {
            log.error("FOP processing error during AFP generation for output {}: {}", outputAfpPath, e.getMessage(), e);
            throw new PermanentException("FOP processing failed for " + outputAfpPath + ": " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IOException during AFP generation for output {}: {}", outputAfpPath, e.getMessage(), e);
            // IOExceptions for file streams could be transient or permanent depending on context
            // For this example, opening output stream or reading input files are critical.
            throw new PermanentException("File I/O error for " + outputAfpPath + ": " + e.getMessage(), e);
        } catch (Exception e) { // Catch other XML transformer exceptions, etc.
            log.error("Unexpected error during AFP generation for output {}: {}", outputAfpPath, e.getMessage(), e);
            throw new PermanentException("Unexpected error during AFP generation for " + outputAfpPath + ": " + e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.warn("Failed to close output stream for {}: {}", outputAfpPath, e.getMessage(), e);
                    // This might be a transient issue, but the main operation might have succeeded or failed already.
                    // Not throwing a new exception here to avoid masking the original one.
                }
            }
            LoggerUtil.clearCorrelationId(); // Clear correlation ID for this operation
        }
    }

    public static void main(String[] args) {
        // Set a correlation ID for the main method execution
        String mainCorrelationId = LoggerUtil.generateCorrelationId();
        LoggerUtil.setCorrelationId(mainCorrelationId);
        
        // Now get a logger instance which will use the logback.xml configuration
        final Logger mainLog = LoggerUtil.getLogger(AfpGenerator.class); // Use class specific logger

        try {
            mainLog.info("AfpGenerator main method started.");
            AfpGenerator generator = new AfpGenerator(); // Constructor logs its own success/failure
            
            // Re-set correlation ID as constructor might have cleared it if it was set only for constructor scope
            LoggerUtil.setCorrelationId(mainCorrelationId); 
            
            String baseDir = "src/main/resources/"; 
            
            File outputDir = new File("output");
            if (!outputDir.exists()){
                if(!outputDir.mkdirs()){
                    mainLog.error("Failed to create output directory: {}", outputDir.getAbsolutePath());
                    // Throwing runtime here as main method cannot easily propagate custom exceptions
                    throw new RuntimeException("Failed to create output directory: " + outputDir.getAbsolutePath());
                }
                mainLog.info("Created output directory: {}", outputDir.getAbsolutePath());
            }
            
            String foTemplatePath = baseDir + "template.fo";
            String xmlDataPath = baseDir + "data.xml";
            String outputFilePath = "output/statement.afp";

            mainLog.debug("Using FO template: {}", foTemplatePath);
            mainLog.debug("Using XML data: {}", xmlDataPath);
            mainLog.debug("Output AFP path: {}", outputFilePath);

            generator.generateAfp(foTemplatePath, xmlDataPath, outputFilePath);
            mainLog.info("Main: AFP generated successfully at " + outputFilePath);

        } catch (CustomApplicationException e) { // Catch our custom exceptions
            mainLog.error("Main: AFP generation failed due to application error: {}", e.getMessage(), e);
        } catch (Exception e) { // Catch any other unexpected exceptions
            mainLog.error("Main: An unexpected error occurred: {}", e.getMessage(), e);
        } finally {
            LoggerUtil.clearCorrelationId(); // Clean up MDC for this main execution
            mainLog.info("AfpGenerator main method finished.");
        }
    }
}
