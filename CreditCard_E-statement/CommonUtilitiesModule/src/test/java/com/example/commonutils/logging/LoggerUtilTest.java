package com.example.commonutils.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class LoggerUtilTest {

    @BeforeEach
    @AfterEach
    void clearMDC() {
        MDC.clear(); // Ensure MDC is clean before and after each test
    }

    @Test
    void testGetLogger() {
        Logger logger = LoggerUtil.getLogger(LoggerUtilTest.class);
        assertNotNull(logger);
        assertEquals(LoggerUtilTest.class.getName(), logger.getName());
    }

    @Test
    void testSetCorrelationId_withValidId() {
        String correlationId = "test-id-123";
        LoggerUtil.setCorrelationId(correlationId);
        assertEquals(correlationId, MDC.get("correlationId"));
    }

    @Test
    void testSetCorrelationId_withNullId_generatesNewId() {
        LoggerUtil.setCorrelationId(null);
        assertNotNull(MDC.get("correlationId"));
        // Could also check UUID format if desired, but not strictly necessary here
    }
    
    @Test
    void testSetCorrelationId_withEmptyId_generatesNewId() {
        LoggerUtil.setCorrelationId("");
        assertNotNull(MDC.get("correlationId"));
        assertTrue(MDC.get("correlationId").length() > 0);
    }
    
    @Test
    void testGetOrGenerateCorrelationId_withExistingId() {
        String existingId = "existing-id-456";
        MDC.put("correlationId", existingId); // Simulate an ID already being there
        
        String returnedId = LoggerUtil.getOrGenerateCorrelationId(existingId);
        assertEquals(existingId, returnedId);
        assertEquals(existingId, MDC.get("correlationId"));
    }

    @Test
    void testGetOrGenerateCorrelationId_withProvidedValidId() {
        String providedId = "provided-id-789";
        String returnedId = LoggerUtil.getOrGenerateCorrelationId(providedId);
        assertEquals(providedId, returnedId);
        assertEquals(providedId, MDC.get("correlationId"));
    }

    @Test
    void testGetOrGenerateCorrelationId_withNullId_generatesAndSetsNewId() {
        String returnedId = LoggerUtil.getOrGenerateCorrelationId(null);
        assertNotNull(returnedId);
        assertEquals(returnedId, MDC.get("correlationId"));
    }
    
    @Test
    void testGetOrGenerateCorrelationId_withEmptyId_generatesAndSetsNewId() {
        String returnedId = LoggerUtil.getOrGenerateCorrelationId("");
        assertNotNull(returnedId);
        assertTrue(returnedId.length() > 0);
        assertEquals(returnedId, MDC.get("correlationId"));
    }


    @Test
    void testGenerateCorrelationId() {
        String id1 = LoggerUtil.generateCorrelationId();
        String id2 = LoggerUtil.generateCorrelationId();
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        // Basic UUID format check (length, hyphens)
        assertTrue(id1.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    @Test
    void testClearCorrelationId() {
        MDC.put("correlationId", "id-to-clear");
        LoggerUtil.clearCorrelationId();
        assertNull(MDC.get("correlationId"));
    }
}
