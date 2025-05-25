package com.example.batchprocessor;

import com.example.afpconversion.model.ConversionResult;
import com.example.afpgen.AfpGenerator;
import com.example.afpconversion.AfpConversionService;
import com.example.emailassembly.EmailAssembler;
import com.example.emailsender.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import jakarta.mail.Session; // For creating dummy MimeMessage
import java.util.Properties; // For creating dummy MimeMessage

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BatchProcessorTest {

    @Mock
    private AfpGenerator mockAfpGenerator;
    @Mock
    private AfpConversionService mockAfpConversionService;
    @Mock
    private EmailAssembler mockEmailAssembler;
    @Mock
    private EmailSender mockEmailSender;

    @InjectMocks // This will inject the mocks into a new BatchProcessor instance
    private BatchProcessor batchProcessor;

    @TempDir
    Path tempDir;

    private Path batchInputPath;
    private Path baseOutputDirPath; // Path to "output_batch"

    @BeforeEach
    void setUp() throws Exception {
        // Create a temporary input JSON file
        batchInputPath = tempDir.resolve("test_batch_input.json");
        try (FileWriter writer = new FileWriter(batchInputPath.toFile())) {
            writer.write("[{\"customerId\": \"TEST001\", \"name\": \"Test User One\", \"statementDate\": \"2024-01-01\", \"email\": \"test1@example.com\"}," +
                         "{\"customerId\": \"TEST002\", \"name\": \"Test User Two\", \"statementDate\": \"2024-01-02\", \"email\": \"test2@example.com\"}]");
        }
        
        // The BatchProcessor creates output in BatchProcessor.BASE_OUTPUT_DIR
        // For tests, we want to ensure this directory is cleaned up.
        // The BatchProcessor itself now cleans up its temp_xml subdir.
        // The main output_batch dir will be created in the project root if running test from there.
        baseOutputDirPath = Paths.get(BatchProcessor.BASE_OUTPUT_DIR);

        // Mock behavior
        // Mock AfpGenerator
        doNothing().when(mockAfpGenerator).generateAfp(anyString(), anyString(), anyString());

        // Mock AfpConversionService
        when(mockAfpConversionService.convertToPdf(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String outputPdfPath = invocation.getArgument(1);
                // Simulate creating the output file
                Files.createDirectories(Paths.get(outputPdfPath).getParent());
                Files.write(Paths.get(outputPdfPath), "dummy pdf content".getBytes());
                return new ConversionResult(true, outputPdfPath, "Dummy PDF created successfully.");
            });
        
        // Mock EmailAssembler
        // Create a dummy MimeMessage for the mock to return
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage dummyMimeMessage = new MimeMessage(session);
        dummyMimeMessage.setSubject("Dummy Subject");

        when(mockEmailAssembler.assembleEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(dummyMimeMessage);

        // Mock EmailSender
        when(mockEmailSender.sendEmail(anyString(), any(MimeMessage.class))).thenReturn(true);
        
        // Ensure the batchProcessor uses the mocked EmailSender with its override
        // (The @InjectMocks and constructor logic in BatchProcessor handles this)
    }

    @Test
    void testProcessBatch_OrchestratesCallsCorrectly() throws Exception {
        batchProcessor.processBatch(batchInputPath.toString());

        // Verify AfpGenerator calls (2 items in JSON)
        verify(mockAfpGenerator, times(2)).generateAfp(anyString(), anyString(), anyString());
        verify(mockAfpGenerator).generateAfp(contains("../AFPGenerationModule/src/main/resources/template.fo"), 
                                            contains("TEST001_20240101"), 
                                            contains("output_batch/afp/TEST001_20240101.afp"));
        verify(mockAfpGenerator).generateAfp(contains("../AFPGenerationModule/src/main/resources/template.fo"), 
                                            contains("TEST002_20240102"), 
                                            contains("output_batch/afp/TEST002_20240102.afp"));


        // Verify AfpConversionService calls
        verify(mockAfpConversionService, times(2)).convertToPdf(anyString(), anyString());
        verify(mockAfpConversionService).convertToPdf(contains("output_batch/afp/TEST001_20240101.afp"), 
                                                    contains("output_batch/pdf/TEST001_20240101.pdf"));
        verify(mockAfpConversionService).convertToPdf(contains("output_batch/afp/TEST002_20240102.afp"), 
                                                    contains("output_batch/pdf/TEST002_20240102.pdf"));

        // Verify EmailAssembler calls
        verify(mockEmailAssembler, times(2)).assembleEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockEmailAssembler).assembleEmail(eq("test1@example.com"), eq("Your Statement for 2024-01-01"), eq("Test User One"), eq("2024-01-01"), contains("output_batch/pdf/TEST001_20240101.pdf"));
        verify(mockEmailAssembler).assembleEmail(eq("test2@example.com"), eq("Your Statement for 2024-01-02"), eq("Test User Two"), eq("2024-01-02"), contains("output_batch/pdf/TEST002_20240102.pdf"));

        // Verify EmailSender calls
        verify(mockEmailSender, times(2)).sendEmail(anyString(), any(MimeMessage.class));
        verify(mockEmailSender).sendEmail(eq("test1@example.com"), any(MimeMessage.class));
        verify(mockEmailSender).sendEmail(eq("test2@example.com"), any(MimeMessage.class));

        // Verify files are created in the (real) output directory structure by the mocks/processor
        assertTrue(Files.exists(baseOutputDirPath.resolve("pdf/TEST001_20240101.pdf")));
        assertTrue(Files.exists(baseOutputDirPath.resolve("eml/TEST001_20240101.eml"))); // Assembled by real processBatch after mock assembler
        assertTrue(Files.exists(baseOutputDirPath.resolve("pdf/TEST002_20240102.pdf")));
        assertTrue(Files.exists(baseOutputDirPath.resolve("eml/TEST002_20240102.eml")));
        
        // Cleanup output_batch (important as it's created in project root by current BatchProcessor design)
        if (Files.exists(baseOutputDirPath)) {
            Files.walk(baseOutputDirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void testProcessBatch_handlesAfpGenerationFailure() throws Exception {
        // Make AfpGenerator throw an exception for the first item
        doThrow(new RuntimeException("Simulated AFP Gen Error"))
            .when(mockAfpGenerator)
            .generateAfp(anyString(), contains("TEST001"), anyString());
        
        // For the second item, let it succeed
        doNothing().when(mockAfpGenerator).generateAfp(anyString(), contains("TEST002"), anyString());


        batchProcessor.processBatch(batchInputPath.toString());

        // Verify AfpGenerator was called for both (or at least first, and then second)
        verify(mockAfpGenerator, times(2)).generateAfp(anyString(), anyString(), anyString());
        
        // Verify that subsequent steps for TEST001 were skipped
        verify(mockAfpConversionService, never()).convertToPdf(contains("TEST001"), anyString());
        verify(mockEmailAssembler, never()).assembleEmail(eq("test1@example.com"), anyString(), anyString(), anyString(), anyString());
        verify(mockEmailSender, never()).sendEmail(eq("test1@example.com"), any(MimeMessage.class));

        // Verify that steps for TEST002 (the successful one) were still called
        verify(mockAfpConversionService).convertToPdf(contains("TEST002"), anyString());
        verify(mockEmailAssembler).assembleEmail(eq("test2@example.com"), anyString(), anyString(), anyString(), anyString());
        verify(mockEmailSender).sendEmail(eq("test2@example.com"), any(MimeMessage.class));

        // Cleanup
        if (Files.exists(baseOutputDirPath)) {
            Files.walk(baseOutputDirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
