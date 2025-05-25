package com.example.restapi.controller;

import com.example.restapi.dto.StatusResponse;
import com.example.restapi.service.StatementService;
import com.example.restapi.config.WebMvcConfig; // Import WebMvcConfig
import com.example.restapi.interceptor.AuthInterceptor; // Import AuthInterceptor
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import; // Import for WebMvcConfig
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatementApiController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class}) // Import the config and interceptor
class StatementApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // This mock will be used by the actual StatementApiController
    private StatementService statementService;

    // --- Tokens for testing ---
    private final String TOKEN_FULL_ACCESS = "Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD";
    private final String TOKEN_READ_ONLY = "Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ";
    private final String TOKEN_DOWNLOAD_ONLY = "Bearer VALID_TOKEN_USER_SCOPE_STATEMENT_DOWNLOAD";
    private final String TOKEN_NO_SCOPES = "Bearer VALID_TOKEN_GUEST";
    private final String TOKEN_INVALID_PREFIX = "Bearer INVALID_TOKEN_PREFIX_OPERATOR_SCOPE_STATEMENT_READ";
    private final String TOKEN_MALFORMED_BEARER = "Bear VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ"; // Malformed Bearer
    private final String TOKEN_NO_BEARER = "VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ";


    @Test
    void getStatementStatus_withValidTokenAndScope_shouldReturnStatus() throws Exception {
        String statementId = "CUST001_20240101"; 
        StatusResponse mockStatus = new StatusResponse(statementId, "Converted_To_PDF", "2024-07-29T10:00:00Z", "PDF is available for download.");
        given(statementService.getStatementStatus(statementId)).willReturn(mockStatus);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/status")
                .header("Authorization", TOKEN_FULL_ACCESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(statementId))
                .andExpect(jsonPath("$.status").value("Converted_To_PDF"));
    }
    
    @Test
    void getStatementStatus_withReadOnlyToken_shouldReturnStatus() throws Exception {
        String statementId = "CUST001_20240101"; 
        StatusResponse mockStatus = new StatusResponse(statementId, "Converted_To_PDF", "2024-07-29T10:00:00Z", "PDF is available for download.");
        given(statementService.getStatementStatus(statementId)).willReturn(mockStatus);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/status")
                .header("Authorization", TOKEN_READ_ONLY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(statementId));
    }


    @Test
    void getStatementStatus_whenNotExists_withValidToken_shouldReturnNotFound() throws Exception {
        String statementId = "UNKNOWN_ID_123";
        given(statementService.getStatementStatus(statementId)).willReturn(null);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/status")
                .header("Authorization", TOKEN_READ_ONLY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STATEMENT_NOT_FOUND"));
    }

    @Test
    void getStatementStatus_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/status"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getStatementStatus_withMalformedBearerToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/status")
                .header("Authorization", TOKEN_MALFORMED_BEARER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatementStatus_withNoBearerToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/status")
                .header("Authorization", TOKEN_NO_BEARER))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void getStatementStatus_withInvalidTokenPrefix_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/status")
                .header("Authorization", TOKEN_INVALID_PREFIX))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStatementStatus_withInsufficientScope_shouldReturnForbidden() throws Exception {
        // TOKEN_DOWNLOAD_ONLY does not have STATEMENT_READ
        mockMvc.perform(get("/api/v1/statements/CUST001/status")
                .header("Authorization", TOKEN_DOWNLOAD_ONLY)) 
                .andExpect(status().isForbidden());
    }
    
    @Test
    void getStatementStatus_withNoScopesToken_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/status")
                .header("Authorization", TOKEN_NO_SCOPES)) 
                .andExpect(status().isForbidden());
    }

    // --- Download Endpoint Tests ---

    @Test
    void downloadStatement_withValidTokenAndScope_shouldReturnFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        String statementId = "CUST001_20240101";
        String format = "pdf";
        Path dummyFile = tempDir.resolve(statementId + "." + format);
        Files.write(dummyFile, "dummy pdf content for download test".getBytes());
        File fileToReturn = dummyFile.toFile();

        given(statementService.getStatementFile(statementId, format)).willReturn(fileToReturn);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/download?format=" + format)
                .header("Authorization", TOKEN_FULL_ACCESS))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileToReturn.getName() + "\""))
                .andExpect(contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("dummy pdf content for download test".getBytes()));
    }
    
    @Test
    void downloadStatement_withDownloadOnlyToken_shouldReturnFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        String statementId = "CUST001_20240101";
        String format = "pdf";
        Path dummyFile = tempDir.resolve(statementId + "." + format);
        Files.write(dummyFile, "dummy pdf content for download test".getBytes());
        File fileToReturn = dummyFile.toFile();

        given(statementService.getStatementFile(statementId, format)).willReturn(fileToReturn);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/download?format=" + format)
                .header("Authorization", TOKEN_DOWNLOAD_ONLY))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileToReturn.getName() + "\""));
    }


    @Test
    void downloadStatement_whenFileNotExists_withValidToken_shouldReturnNotFound(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        String statementId = "NO_DOWNLOAD_FILE_ID";
        String format = "pdf";
        given(statementService.getStatementFile(statementId, format)).willReturn(null);

        mockMvc.perform(get("/api/v1/statements/" + statementId + "/download?format=" + format)
                .header("Authorization", TOKEN_DOWNLOAD_ONLY))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void downloadStatement_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadStatement_withInvalidTokenPrefix_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/download")
                .header("Authorization", TOKEN_INVALID_PREFIX))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadStatement_withInsufficientScope_shouldReturnForbidden() throws Exception {
        // TOKEN_READ_ONLY does not have STATEMENT_DOWNLOAD
        mockMvc.perform(get("/api/v1/statements/CUST001/download")
                .header("Authorization", TOKEN_READ_ONLY))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void downloadStatement_withNoScopesToken_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/statements/CUST001/download")
                .header("Authorization", TOKEN_NO_SCOPES))
                .andExpect(status().isForbidden());
    }
}
