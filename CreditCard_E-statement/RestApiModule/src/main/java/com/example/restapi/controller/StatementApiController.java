package com.example.restapi.controller;

import com.example.restapi.dto.ErrorResponse;
import com.example.restapi.dto.StatusResponse;
import com.example.restapi.service.StatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementApiController {

    @Autowired
    private StatementService statementService;

    @GetMapping("/{statement_id}/status")
    public ResponseEntity<?> getStatementStatus(@PathVariable("statement_id") String statementId) {
        StatusResponse status = statementService.getStatementStatus(statementId);
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new ErrorResponse("STATEMENT_NOT_FOUND", "Statement with ID '" + statementId + "' not found."));
        }
    }

    @GetMapping("/{statement_id}/download")
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable("statement_id") String statementId,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        
        File file = statementService.getStatementFile(statementId, format);

        if (file != null && file.exists()) {
            FileSystemResource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            // Corrected to include quotes around the filename
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\""); 
            
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if ("pdf".equalsIgnoreCase(format)) {
                mediaType = MediaType.APPLICATION_PDF;
            }
            // Add more format handlers if needed (e.g., "afp" -> "application/afp")

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(mediaType)
                    .body(resource);
        } else {
            // For consistency, return a JSON error response for file not found as well
            // This can be handled by an exception handler or directly here.
            // The task description implies ResponseEntity.notFound().build();
            return ResponseEntity.notFound().build(); 
        }
    }
}
