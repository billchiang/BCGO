package com.example.batchprocessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException; // Not strictly needed for this version of the test
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Comparator; // For cleanup

class BatchProcessorTest {

    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    @Test
    void testProcessBatch() throws Exception {
        BatchProcessor processor = new BatchProcessor();

        // Create a temporary input JSON file
        File batchInputFile = tempDir.resolve("test_batch_input.json").toFile();
        try (FileWriter writer = new FileWriter(batchInputFile)) {
            writer.write("[{\"customerId\": \"TEST001\", \"name\": \"Test User\", \"statementDate\": \"2024-01-01\", \"email\": \"test@example.com\"}," +
                         "{\"customerId\": \"TEST002\", \"name\": \"Another User\", \"statementDate\": \"2024-01-02\", \"email\": \"another@example.com\"}]");
        }

        // The BatchProcessor creates output in "output_batch" relative to its execution directory.
        // For tests, it's generally better if this output also goes into the @TempDir
        // to ensure automatic cleanup and avoid polluting the project structure.
        // However, the current BatchProcessor design hardcodes "output_batch".
        // So, we'll assert based on that and perform manual cleanup.
        Path projectRootDefaultOutputDir = Paths.get(BatchProcessor.BASE_OUTPUT_DIR);

        try {
            processor.processBatch(batchInputFile.getAbsolutePath());

            // Verify output files
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("afp/TEST001_20240101.afp")), "AFP file for TEST001 should exist.");
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("pdf/TEST001_20240101.pdf")), "PDF file for TEST001 should exist.");
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("eml/TEST001_20240101.eml")), "EML file for TEST001 should exist.");
            
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("afp/TEST002_20240102.afp")), "AFP file for TEST002 should exist.");
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("pdf/TEST002_20240102.pdf")), "PDF file for TEST002 should exist.");
            assertTrue(Files.exists(projectRootDefaultOutputDir.resolve("eml/TEST002_20240102.eml")), "EML file for TEST002 should exist.");
            
            // Basic content check for one file to ensure it's not just an empty touch
            String content = new String(Files.readAllBytes(projectRootDefaultOutputDir.resolve("afp/TEST001_20240101.afp")));
            assertTrue(content.contains("AFP content for TEST001"), "AFP file content mismatch.");

        } finally {
            // Cleanup - delete the output_batch directory created by the test
            if (Files.exists(projectRootDefaultOutputDir)) {
                Files.walk(projectRootDefaultOutputDir)
                    .sorted(Comparator.reverseOrder()) // Sort in reverse to delete files before directories
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }
}
