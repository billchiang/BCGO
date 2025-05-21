package com.example.afpconversion;

import com.example.afpconversion.model.ConversionRequest;
import com.example.afpconversion.model.ConversionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class AfpConversionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AfpConversionService.class);

    // Keep @Value for other potential configurations, but for script path, we'll make it more explicit for now
    // For a real Spring Boot app, @Value would be fine.
    // @Value("${afp.conversion.tool.executable.path}")
    private String conversionToolExecutablePath = "scripts/run_conversion_tool.sh.example"; // Relative to module root

    @Value("${afp.resources.font.path:./dummy_font_path}") // Example default
    private String afpFontPath;

    @Value("${afp.resources.overlay.path:./dummy_overlay_path}") // Example default
    private String afpOverlayPath;

    @Value("${afp.resources.pseg.path:./dummy_pseg_path}") // Example default
    private String afpPsegPath;

    @Value("${afp.conversion.temp.storage.path:/tmp/afp_conversion}")
    private String tempStoragePath;

    /**
     * Converts an AFP file to PDF using an external script.
     *
     * @param afpFilePath Path to the input AFP file.
     * @param outputPdfPath Path for the output PDF file.
     * @return ConversionResult indicating success or failure.
     */
    public ConversionResult convertToPdf(String afpFilePath, String outputPdfPath) {
        LOGGER.info("Request received to convert AFP {} to PDF {}", afpFilePath, outputPdfPath);
        
        File inputFile = new File(afpFilePath);
        if (!inputFile.exists()) {
            LOGGER.error("Input AFP file not found: {}", afpFilePath);
            return new ConversionResult(false, null, "Input AFP file not found: " + afpFilePath);
        }

        // Ensure the script path is absolute or relative to a known base directory.
        // For this example, assume the script is in "scripts/" relative to the module's root.
        File scriptFile = new File(conversionToolExecutablePath);
        if (!scriptFile.exists()) {
            LOGGER.error("Conversion script not found at: {}", scriptFile.getAbsolutePath());
            return new ConversionResult(false, null, "Conversion script not found: " + scriptFile.getAbsolutePath());
        }
        if (!scriptFile.canExecute()) {
             LOGGER.warn("Conversion script at {} is not executable. Attempting to set it.", scriptFile.getAbsolutePath());
             scriptFile.setExecutable(true); // Try to make it executable
             if (!scriptFile.canExecute()) {
                LOGGER.error("Failed to make conversion script executable at: {}", scriptFile.getAbsolutePath());
                return new ConversionResult(false, null, "Conversion script not executable: " + scriptFile.getAbsolutePath());
             }
        }


        ProcessBuilder processBuilder = new ProcessBuilder(
                scriptFile.getAbsolutePath(), // Use absolute path to script
                inputFile.getAbsolutePath(),  // Arg 1: input AFP path
                new File(outputPdfPath).getAbsolutePath(), // Arg 2: output PDF path
                "pdf"                         // Arg 3: format string
        );

        // Redirect error stream to output stream to capture all output together
        processBuilder.redirectErrorStream(true);
        
        LOGGER.info("Executing conversion command: {}", String.join(" ", processBuilder.command()));

        try {
            Process process = processBuilder.start();
            
            // Capture output from the script
            StringBuilder scriptOutput = new StringBuilder();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    scriptOutput.append(new String(buffer, 0, length));
                }
            }
            LOGGER.info("Conversion script output:\n{}", scriptOutput.toString());

            boolean exited = process.waitFor(30, TimeUnit.SECONDS); // Wait for 30 seconds
            if (!exited) {
                process.destroyForcibly();
                LOGGER.error("Conversion script timed out.");
                return new ConversionResult(false, null, "Conversion script timed out.");
            }

            int exitCode = process.exitValue();
            File outputFile = new File(outputPdfPath);

            if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) { // Check if file exists and is not empty
                LOGGER.info("Conversion successful: Output PDF created at {}", outputPdfPath);
                return new ConversionResult(true, outputPdfPath, "Conversion successful.");
            } else if (exitCode == 0 && (!outputFile.exists() || outputFile.length() == 0)) {
                 LOGGER.error("Conversion script reported success (exit code 0) but output file {} is missing or empty.", outputPdfPath);
                return new ConversionResult(false, null, "Conversion script reported success but output file is missing or empty.");
            }
            else {
                LOGGER.error("Conversion script failed with exit code: {}. Output: {}", exitCode, scriptOutput.toString().trim());
                return new ConversionResult(false, null, "Conversion script failed. Exit code: " + exitCode + ". Details: " + scriptOutput.toString().trim());
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error executing conversion script.", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return new ConversionResult(false, null, "Error executing conversion script: " + e.getMessage());
        }
    }

    // Other conversion methods (convertToPdfA, convertToPcl, etc.) would remain
    // and could be implemented similarly if scripts for them are created.
    // For now, they will use a simple dummy implementation.

    public ConversionResult convertToPdfA(String afpFilePath, String outputPdfAPath) {
        LOGGER.info("Request received to convert AFP {} to PDF/A {}. (Dummy implementation)", afpFilePath, outputPdfAPath);
        return executeDummyConversion(new ConversionRequest(afpFilePath, "pdfa", outputPdfAPath));
    }

    public ConversionResult convertToPcl(String afpFilePath, String outputPclPath) {
        LOGGER.info("Request received to convert AFP {} to PCL {}. (Dummy implementation)", afpFilePath, outputPclPath);
        return executeDummyConversion(new ConversionRequest(afpFilePath, "pcl", outputPclPath));
    }

    public ConversionResult convertToPostScript(String afpFilePath, String outputPsPath) {
        LOGGER.info("Request received to convert AFP {} to PostScript {}. (Dummy implementation)", afpFilePath, outputPsPath);
         return executeDummyConversion(new ConversionRequest(afpFilePath, "ps", outputPsPath));
    }
    
    private ConversionResult executeDummyConversion(ConversionRequest request) {
        File inputFile = new File(request.getInputFilePath());
        if (!inputFile.exists()) {
            LOGGER.error("Input AFP file not found: {}", request.getInputFilePath());
            return new ConversionResult(false, null, "Input AFP file not found: " + request.getInputFilePath());
        }
        try {
            Path outputFilePathObj = Paths.get(request.getOutputFilePath());
            File outputDir = outputFilePathObj.getParent().toFile();
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File outputFile = new File(request.getOutputFilePath());
            if (outputFile.createNewFile() || outputFile.exists()) { // Ensure file is created or already exists
                 // For dummy, create a small file to pass length check
                java.nio.file.Files.write(outputFile.toPath(), "dummy content".getBytes());
                LOGGER.info("Dummy conversion: Created placeholder output file at {}", request.getOutputFilePath());
                return new ConversionResult(true, request.getOutputFilePath(), "Dummy conversion successful.");
            } else {
                 LOGGER.error("Dummy conversion failed to create output file: {}", request.getOutputFilePath());
                return new ConversionResult(false, null, "Dummy conversion failed to create output file.");
            }
        } catch (IOException e) {
            LOGGER.error("Dummy conversion failed to create output file: {}", e.getMessage());
            return new ConversionResult(false, null, "Dummy conversion failed: " + e.getMessage());
        }
    }

    // Main method for demonstration
    public static void main(String[] args) {
        // Create a dummy AfpConversionService instance (not managed by Spring here)
        AfpConversionService service = new AfpConversionService();
        
        // Define input and output paths.
        // The input AFP is expected to be generated by the AFPGenerationModule.
        // We need to ensure this path is correct relative to where this main method is run.
        // Assuming this module (AFP_Conversion_Module_Design) is a sibling to AFPGenerationModule
        // and both are under CreditCard_E-statement
        String inputAfp = "../AFPGenerationModule/output/statement.afp"; 
        String outputPdf = "output/statement.pdf"; // Output within AFP_Conversion_Module_Design/output

        // Create a dummy input AFP file if it doesn't exist for the main method to run standalone
        File dummyInputAfpFile = new File(inputAfp);
        try {
            if (!dummyInputAfpFile.exists()) {
                dummyInputAfpFile.getParentFile().mkdirs();
                java.nio.file.Files.write(dummyInputAfpFile.toPath(), "dummy AFP content".getBytes());
                System.out.println("Created dummy input AFP file: " + dummyInputAfpFile.getAbsolutePath());
            } else {
                 System.out.println("Using existing input AFP file: " + dummyInputAfpFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create dummy input AFP file: " + e.getMessage());
            // e.printStackTrace(); // For more detail
            // return; // Optionally exit if input can't be ensured
        }


        System.out.println("Attempting AFP to PDF conversion...");
        System.out.println("Input AFP: " + new File(inputAfp).getAbsolutePath());
        System.out.println("Output PDF: " + new File(outputPdf).getAbsolutePath());

        ConversionResult result = service.convertToPdf(inputAfp, outputPdf);

        System.out.println("\n--- Conversion Result ---");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Output File: " + result.getOutputFilePath());
        System.out.println("Message: " + result.getErrorMessage());

        if (result.isSuccess()) {
            System.out.println("\nTo verify, check the output file at: " + new File(result.getOutputFilePath()).getAbsolutePath());
        } else {
            System.out.println("\nConversion failed. Check logs for details.");
        }
    }
}
