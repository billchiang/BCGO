package com.example.restapi.dto;
// No import for LocalDateTime needed if using String for lastUpdatedAt
// import java.time.LocalDateTime; 

public class StatusResponse {
    public String statementId;
    public String status;
    public String lastUpdatedAt; // Using String for simplicity as per task
    public String details;

    public StatusResponse(String statementId, String status, String lastUpdatedAt, String details) {
        this.statementId = statementId;
        this.status = status;
        this.lastUpdatedAt = lastUpdatedAt;
        this.details = details;
    }

    // Getters (and potentially setters) can be added if needed, 
    // but Spring Boot can often work with public fields for DTOs.
    // For robustness and if using frameworks like Jackson more explicitly, getters are good practice.

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
