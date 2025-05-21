package com.example.afpconversion.model;

public class ConversionResult {
    private boolean success;
    private String outputFilePath;
    private String errorMessage;

    public ConversionResult(boolean success, String outputFilePath, String errorMessage) {
        this.success = success;
        this.outputFilePath = outputFilePath;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ConversionResult{" +
                "success=" + success +
                ", outputFilePath='" + outputFilePath + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
