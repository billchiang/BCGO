package com.example.restapi.service;
import com.example.restapi.dto.StatusResponse;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StatementService {
    // Base path for where dummy generated files are stored by other modules (relative to project root)
    private static final Path BATCH_OUTPUT_PDF_DIR = Paths.get("../BatchProcessingModule/output_batch/pdf/");
    private static final Path BATCH_OUTPUT_AFP_DIR = Paths.get("../BatchProcessingModule/output_batch/afp/");


    public StatusResponse getStatementStatus(String statementId) {
        // Simulate checking if a PDF exists from the batch output
        // statementId might be CUST001_20240310
        File pdfFile = BATCH_OUTPUT_PDF_DIR.resolve(statementId + ".pdf").toFile();
        File afpFile = BATCH_OUTPUT_AFP_DIR.resolve(statementId + ".afp").toFile();

        if (pdfFile.exists()) {
            return new StatusResponse(statementId, "Converted_To_PDF", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), "PDF is available for download.");
        } else if (afpFile.exists()) {
            return new StatusResponse(statementId, "AFP_Generated", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), "AFP generated, awaiting PDF conversion.");
        } else {
            return null; // Will be handled as 404 by controller
        }
    }

    public File getStatementFile(String statementId, String format) {
        // For now, only support PDF from the batch output simulation
        if ("pdf".equalsIgnoreCase(format)) {
            File pdfFile = BATCH_OUTPUT_PDF_DIR.resolve(statementId + ".pdf").toFile();
            if (pdfFile.exists()) {
                return pdfFile;
            }
        }
        return null; // Will be handled as 404
    }
}
