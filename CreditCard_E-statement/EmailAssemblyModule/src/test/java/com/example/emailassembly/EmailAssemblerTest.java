package com.example.emailassembly;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EmailAssemblerTest {

    @Test
    void testAssembleEmail(@TempDir Path tempDir) throws Exception {
        EmailAssembler assembler = new EmailAssembler();

        // Create a dummy PDF file for attachment
        Path dummyPdf = tempDir.resolve("statement.pdf");
        Files.write(dummyPdf, "dummy PDF content".getBytes());

        String to = "test@example.com";
        String subject = "Test Subject";
        String customerName = "Test Customer";
        String statementDate = "2024-01-01";

        MimeMessage message = assembler.assembleEmail(to, subject, customerName, statementDate, dummyPdf.toString());

        assertEquals(subject, message.getSubject());
        assertEquals(to, message.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("noreply@example.com", message.getFrom()[0].toString());

        Object content = message.getContent();
        assertTrue(content instanceof Multipart);

        Multipart multipart = (Multipart) content;
        assertEquals(2, multipart.getCount(), "Should have two parts: HTML body and PDF attachment");

        // Part 1: HTML Body
        BodyPart htmlPart = multipart.getBodyPart(0);
        assertTrue(htmlPart.isMimeType("text/html"));
        String htmlContent = (String) htmlPart.getContent();
        assertTrue(htmlContent.contains(customerName));
        assertTrue(htmlContent.contains(statementDate));
        
        // Part 2: PDF Attachment
        BodyPart pdfPart = multipart.getBodyPart(1);
        // Check main type, ignore charset if present (getContentType() can return "application/pdf; charset=UTF-8" or similar)
        assertTrue(pdfPart.getContentType().toLowerCase().startsWith("application/pdf")); 
        assertEquals(dummyPdf.getFileName().toString(), pdfPart.getFileName());
    }
}
