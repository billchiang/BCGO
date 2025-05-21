package com.example.batchprocessor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BatchProcessor {

    private static final String BASE_OUTPUT_DIR = "output_batch"; // Relative to module root

    public void processBatch(String batchInputJsonFilePath) throws IOException {
        Gson gson = new Gson();
        Type batchItemListType = new TypeToken<List<BatchItem>>() {}.getType();
        
        // Ensure base output directory exists
        Files.createDirectories(Paths.get(BASE_OUTPUT_DIR, "afp"));
        Files.createDirectories(Paths.get(BASE_OUTPUT_DIR, "pdf"));
        Files.createDirectories(Paths.get(BASE_OUTPUT_DIR, "eml"));

        try (FileReader reader = new FileReader(batchInputJsonFilePath)) {
            List<BatchItem> items = gson.fromJson(reader, batchItemListType);
            
            int totalItems = items.size();
            int afpGenerated = 0;
            int pdfConverted = 0;
            int emlAssembled = 0;
            int emailSent = 0;

            System.out.println("Starting batch processing for " + totalItems + " items...");

            for (BatchItem item : items) {
                System.out.println("Processing item for customer: " + item.customerId);
                String baseFileName = item.customerId + "_" + item.statementDate.replace("-", "");

                // Simulate AFP Generation
                String afpFileName = Paths.get(BASE_OUTPUT_DIR, "afp", baseFileName + ".afp").toString();
                try (FileWriter fw = new FileWriter(afpFileName)) {
                    fw.write("AFP content for " + item.customerId);
                    System.out.println("  Generated AFP: " + afpFileName);
                    afpGenerated++;
                } catch (IOException e) {
                    System.err.println("  Failed to generate AFP for " + item.customerId + ": " + e.getMessage());
                }

                // Simulate PDF Conversion
                String pdfFileName = Paths.get(BASE_OUTPUT_DIR, "pdf", baseFileName + ".pdf").toString();
                try (FileWriter fw = new FileWriter(pdfFileName)) {
                    fw.write("PDF content for " + item.customerId); // Dummy PDF
                    System.out.println("  Converted to PDF: " + pdfFileName);
                    pdfConverted++;
                } catch (IOException e) {
                    System.err.println("  Failed to convert PDF for " + item.customerId + ": " + e.getMessage());
                }
                
                // Simulate Email Assembly
                String emlFileName = Paths.get(BASE_OUTPUT_DIR, "eml", baseFileName + ".eml").toString();
                 try (FileWriter fw = new FileWriter(emlFileName)) {
                    fw.write("To: " + item.email + "\n");
                    fw.write("Subject: Statement for " + item.name + "\n\n");
                    fw.write("This is the EML content.");
                    System.out.println("  Assembled EML: " + emlFileName);
                    emlAssembled++;
                } catch (IOException e) {
                    System.err.println("  Failed to assemble EML for " + item.customerId + ": " + e.getMessage());
                }

                // Simulate Email Sending
                System.out.println("  Simulating email send for " + item.customerId + " to " + item.email);
                emailSent++; // Assume success for simulation
            }

            System.out.println("\n--- Batch Processing Summary ---");
            System.out.println("Total items processed: " + totalItems);
            System.out.println("AFPs generated: " + afpGenerated);
            System.out.println("PDFs converted: " + pdfConverted);
            System.out.println("EMLs assembled: " + emlAssembled);
            System.out.println("Emails 'sent': " + emailSent);
            System.out.println("------------------------------");
            System.out.println("Output files are in: " + new File(BASE_OUTPUT_DIR).getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        try {
            BatchProcessor processor = new BatchProcessor();
            // Default path relative to module root
            String inputFile = "src/main/resources/batch_input.json"; 
            if (args.length > 0) {
                inputFile = args[0]; // Allow overriding via command line arg
            }
            System.out.println("Processing batch from input file: " + inputFile);
            processor.processBatch(inputFile);
        } catch (Exception e) {
            // e.printStackTrace(); // More detailed for dev, simpler for user
            System.err.println("Error during batch processing: " + e.getMessage());
        }
    }
}
