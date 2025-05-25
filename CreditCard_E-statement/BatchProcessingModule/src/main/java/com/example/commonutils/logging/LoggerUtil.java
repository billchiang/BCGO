package com.example.commonutils.logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.UUID;

public class LoggerUtil {
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void setCorrelationId(String correlationId) {
        // Ensure a correlation ID is always set if this method is called,
        // or if an existing one is provided, use it.
        if (correlationId == null || correlationId.trim().isEmpty()) {
            MDC.put("correlationId", generateCorrelationId());
        } else {
            MDC.put("correlationId", correlationId);
        }
    }
    
    public static String getOrGenerateCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            String newId = generateCorrelationId();
            MDC.put("correlationId", newId);
            return newId;
        } else {
            MDC.put("correlationId", correlationId);
            return correlationId;
        }
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public static void clearCorrelationId() {
        MDC.remove("correlationId");
    }
}
