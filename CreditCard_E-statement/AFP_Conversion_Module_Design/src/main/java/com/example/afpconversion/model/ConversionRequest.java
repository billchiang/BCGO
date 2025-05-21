package com.example.afpconversion.model;

public class ConversionRequest {
    private String inputFilePath;
    private String outputFormat; // e.g., "pdf", "pdfa", "pcl", "ps"
    private String outputFilePath;
    // Optional: Add other parameters like pdfa_compliance_level, ocr_required, etc.

    public ConversionRequest(String inputFilePath, String outputFormat, String outputFilePath) {
        this.inputFilePath = inputFilePath;
        this.outputFormat = outputFormat;
        this.outputFilePath = outputFilePath;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    @Override
    public String toString() {
        return "ConversionRequest{" +
                "inputFilePath='" + inputFilePath + '\'' +
                ", outputFormat='" + outputFormat + '\'' +
                ", outputFilePath='" + outputFilePath + '\'' +
                '}';
    }
}
