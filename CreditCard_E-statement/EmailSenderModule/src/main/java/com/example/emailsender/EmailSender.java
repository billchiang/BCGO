package com.example.emailsender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.xbill.DNS.*;
import org.xbill.DNS.Record; // Explicit import to avoid collision

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties; 

public class EmailSender {

    private String heloDomain = "my-smtp-client.example.com"; // Configurable
    private String mailFrom = "sender@example.com";         // Configurable

    // For testing - allow overriding target SMTP server
    private String overrideSmtpHost = null;
    private int overrideSmtpPort = 25;

    public EmailSender() {}

    public EmailSender(String mailFrom, String heloDomain) {
        this.mailFrom = mailFrom;
        this.heloDomain = heloDomain;
    }
    
    public void setOverrideSmtpServer(String host, int port) {
        this.overrideSmtpHost = host;
        this.overrideSmtpPort = port;
    }

    public boolean sendEmail(String recipientEmail, MimeMessage message) {
        String domain = recipientEmail.substring(recipientEmail.indexOf("@") + 1);
        List<String> mxHosts;

        if (overrideSmtpHost != null) {
            mxHosts = Arrays.asList(overrideSmtpHost);
            System.out.println("Using override SMTP server: " + overrideSmtpHost + ":" + overrideSmtpPort);
        } else {
            try {
                mxHosts = getMXHosts(domain);
                if (mxHosts.isEmpty()) {
                    System.err.println("No MX records found for domain: " + domain);
                    return false;
                }
            } catch (TextParseException e) {
                System.err.println("Error parsing domain for MX lookup: " + domain);
                // e.printStackTrace(); // Usually too verbose for library use
                return false;
            }
        }
        
        for (String host : mxHosts) {
            int port = (overrideSmtpHost != null ? overrideSmtpPort : 25);
            System.out.println("Attempting to send email via: " + host + ":" + port);
            try (Socket socket = new Socket(host, port);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")); // Specify UTF-8
                 OutputStream os = socket.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) { // Specify UTF-8 & auto-flush

                socket.setSoTimeout(10000); // 10 seconds timeout for read operations

                if (!expectCode(reader, "220")) return false;

                writer.println("HELO " + heloDomain);
                if (!expectCode(reader, "250")) return false;

                // Optional: Implement STARTTLS here if needed
                // writer.println("STARTTLS");
                // if (expectCode(reader, "220")) { /* proceed with TLS handshake */ }

                writer.println("MAIL FROM:<" + mailFrom + ">");
                if (!expectCode(reader, "250")) return false;

                writer.println("RCPT TO:<" + recipientEmail + ">");
                if (!expectCode(reader, "250")) return false; // Could also be 251

                writer.println("DATA");
                if (!expectCode(reader, "354")) return false;

                // Use MimeMessage.writeTo() to send the complete message content
                // Ensure headers are terminated with CRLF, and body is separated by CRLF.
                // MimeMessage.writeTo should handle this correctly.
                message.writeTo(os); 
                os.flush(); // Ensure all of MimeMessage is sent

                // Send the DATA termination sequence CRLF . CRLF
                // Using OutputStream directly for this is more reliable than PrintWriter for specific byte sequences.
                os.write(new byte[]{'\r', '\n', '.', '\r', '\n'});
                os.flush();

                if (!expectCode(reader, "250")) return false; // Response to DATA termination

                writer.println("QUIT");
                expectCode(reader, "221"); // Server closes connection

                System.out.println("Email sent successfully to " + recipientEmail + " via " + host);
                return true;

            } catch (IOException | MessagingException e) {
                System.err.println("Error sending email via " + host + ": " + e.getMessage());
                // e.printStackTrace(); // for more detail during development
            }
        }
        System.err.println("Failed to send email to " + recipientEmail + " via all MX hosts.");
        return false;
    }

    private List<String> getMXHosts(String domain) throws TextParseException {
        List<String> hosts = new ArrayList<>();
        Lookup lookup = new Lookup(domain, Type.MX);
        Record[] records = lookup.run();

        if (lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0) {
            List<MXRecord> mxRecords = new ArrayList<>();
            for (Record record : records) {
                if (record instanceof MXRecord) { 
                    mxRecords.add((MXRecord) record);
                }
            }
            if (mxRecords.isEmpty()){
                 System.err.println("No MX records of type MXRecord found, despite successful lookup for " + domain);
            }
            mxRecords.sort(Comparator.comparingInt(MXRecord::getPriority));
            for (MXRecord mx : mxRecords) {
                hosts.add(mx.getTarget().toString(true)); 
            }
        } 
        
        if (hosts.isEmpty()) { // Fallback or if initial MX lookup wasn't successful
             System.err.println("MX Lookup failed or returned no valid records for " + domain + ": " + lookup.getErrorString() + ". Attempting A record fallback.");
             lookup = new Lookup(domain, Type.A);
             records = lookup.run();
             if(lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0) {
                 for(Record record : records) {
                     if (record instanceof ARecord) { 
                         hosts.add(((ARecord)record).getAddress().getHostAddress());
                     }
                 }
                 if(hosts.isEmpty()){
                    System.err.println("A Record Lookup for " + domain + " was successful but found no ARecord instances.");
                 }
             } else {
                 System.err.println("A Record Lookup also failed for " + domain + ": " + lookup.getErrorString());
             }
        }
        return hosts;
    }

    private boolean expectCode(BufferedReader reader, String expectedCode) throws IOException {
        String line = reader.readLine();
        System.out.println("SERVER: " + line); // Log server response
        if (line == null) {
            System.err.println("Server closed connection unexpectedly. Expected " + expectedCode);
            return false;
        }
        if (!line.startsWith(expectedCode)) {
            System.err.println("SMTP Error: Expected " + expectedCode + " but got: " + line);
            return false;
        }
        // Handle multi-line responses if necessary (e.g., for EHLO response)
        // For simple 220, 250, 354, 221, usually single line is primary.
        while (line.charAt(3) == '-') { // Check for hyphen indicating multi-line
            line = reader.readLine();
            System.out.println("SERVER: " + line); // Log multi-line part
            if (line == null) {
                 System.err.println("Server closed connection unexpectedly during multi-line response. Expected " + expectedCode);
                 return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        EmailSender sender = new EmailSender("test@mydomain.com", "my-test-client.example.com");
        
        sender.setOverrideSmtpServer("localhost", 1025); 

        try {
            Properties props = new Properties();
            jakarta.mail.Session session = jakarta.mail.Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress("test@mydomain.com"));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, 
                                  jakarta.mail.internet.InternetAddress.parse("recipient@localhost"));
            message.setSubject("Test Email from Custom SMTP Sender");
            message.setText("Hello,\n\nThis is a test email sent via custom SMTP logic!\n\nRegards,\nTester");
            message.setHeader("X-Custom-Header", "TestValue123");
            message.saveChanges(); 

            System.out.println("Sending test email...");
            boolean success = sender.sendEmail("recipient@localhost", message); // Use a simple recipient for override
            System.out.println("\nMain: Email send attempt result: " + success);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
