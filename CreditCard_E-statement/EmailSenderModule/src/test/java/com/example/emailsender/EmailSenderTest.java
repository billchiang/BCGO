package com.example.emailsender;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup; // Changed from ServerSetupTest for clarity
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


class EmailSenderTest {

    @RegisterExtension
    // Use a specific, non-default port for SMTP to avoid conflicts and permission issues.
    static ServerSetup smtpSetup = new ServerSetup(3025, null, ServerSetup.PROTOCOL_SMTP);
    static GreenMailExtension greenMail = new GreenMailExtension(smtpSetup)
                                                .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());


    @Test
    void testSendEmailWithGreenMail() throws Exception {
        EmailSender sender = new EmailSender("sender@example.com", "test.client.com");
        
        String host = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();
        sender.setOverrideSmtpServer(host, port);
        System.out.println("GreenMail SMTP listening on: " + host + ":" + port);


        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress("testrecipient@localhost"));
        message.setSubject("Test Email via GreenMail");
        message.setText("This is a test message body.");
        message.saveChanges(); 

        boolean success = sender.sendEmail("testrecipient@localhost", message);
        assertTrue(success, "Email should be sent successfully to GreenMail");

        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "Should receive 1 email"); 
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);
        
        MimeMessage receivedMessage = receivedMessages[0];
        assertEquals("Test Email via GreenMail", receivedMessage.getSubject());
        
        Object contentObject = receivedMessage.getContent();
        String content = "";
        if (contentObject instanceof String) {
            content = (String) contentObject;
        } else if (contentObject instanceof InputStream) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) contentObject))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
        } else if (contentObject != null) { // Fallback for other content types like MimeMultipart
             content = contentObject.toString(); // This might not be the raw text, but good enough for a simple check
        }
        
        assertTrue(content.trim().contains("This is a test message body."), "Email content mismatch. Actual: " + content);
        assertEquals("testrecipient@localhost", receivedMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("sender@example.com", ((InternetAddress) receivedMessage.getFrom()[0]).getAddress());
    }
}
