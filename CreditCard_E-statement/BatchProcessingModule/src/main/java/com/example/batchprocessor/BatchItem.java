package com.example.batchprocessor;

// Using public fields for simplicity with Gson
// For a more robust application, private fields with getters/setters would be typical.
public class BatchItem {
    public String customerId;
    public String name;
    public String statementDate;
    public String email;
    // For simplicity, the "items" array from JSON is not mapped here.
    // If needed, you would define an ItemDetail class and a List<ItemDetail> items;
    // public List<ItemDetail> items;
    // public static class ItemDetail { public String item; public String amount; }

    @Override
    public String toString() {
        return "BatchItem{" +
                "customerId='" + customerId + '\'' +
                ", name='" + name + '\'' +
                ", statementDate='" + statementDate + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
