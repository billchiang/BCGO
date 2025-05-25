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

                // MAIL FROM and RCPT TO should be based on the MimeMessage's envelope if possible,
                // or use the provided parameters. For this version, we use provided `mailFrom`
                // and the `recipientEmail` parameter for RCPT TO.
                // The actual MimeMessage headers (From, To) will be part of the DATA.
                String actualMailFrom = this.mailFrom; // Could also extract from MimeMessage if needed
                if (message.getFrom() != null && message.getFrom().length > 0) {
                     // For consistency, let's use the MimeMessage's From if available,
                     // but ensure the envelope sender (MAIL FROM) is still controlled.
                     // For now, sticking to the class field 'this.mailFrom' for MAIL FROM command.
                }

                writer.println("MAIL FROM:<" + actualMailFrom + ">");
                if (!expectCode(reader, "250")) return false;

                writer.println("RCPT TO:<" + recipientEmail + ">");
                if (!expectCode(reader, "250")) return false; // Could also be 251

                writer.println("DATA");
                if (!expectCode(reader, "354")) return false;

                message.writeTo(os); 
                os.flush(); 

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
        
        if (hosts.isEmpty()) { 
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
        System.out.println("SERVER: " + line); 
        if (line == null) {
            System.err.println("Server closed connection unexpectedly. Expected " + expectedCode);
            return false;
        }
        if (!line.startsWith(expectedCode)) {
            System.err.println("SMTP Error: Expected " + expectedCode + " but got: " + line);
            return false;
        }
        while (line.charAt(3) == '-') { 
            line = reader.readLine();
            System.out.println("SERVER: " + line); 
            if (line == null) {
                 System.err.println("Server closed connection unexpectedly during multi-line response. Expected " + expectedCode);
                 return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        // Use the EmailSender's own mailFrom and heloDomain for the envelope
        EmailSender sender = new EmailSender("sender-from-actual-send@example.com", "email-sender-module.example.com");
        
        // For testing with a local dummy SMTP server like Python's smtpd:
        // python -m smtpd -n -c DebuggingServer localhost:1025
        sender.setOverrideSmtpServer("localhost", 1025); 

        // Path to the .eml file generated by EmailAssemblyModule
        String emlFilePath = "../../EmailAssemblyModule/output/assembled_email.eml";
        File emlFile = new File(emlFilePath);

        if (!emlFile.exists()) {
            System.err.println("EML file not found at: " + emlFile.getAbsolutePath());
            System.err.println("Please ensure EmailAssemblyModule has been run and produced assembled_email.eml.");
            
            // Create a dummy EML file for basic test continuity if it's missing
            System.out.println("Attempting to create a dummy EML file for testing...");
            try {
                emlFile.getParentFile().mkdirs(); // Ensure parent directory exists
                try (FileWriter fw = new FileWriter(emlFile)) {
                    fw.write("To: dummyrecipient@localhost\n");
                    fw.write("From: dummysender@example.com\n");
                    fw.write("Subject: Dummy EML for EmailSender Test\n\n");
                    fw.write("This is a dummy EML body because the original was not found.");
                }
                System.out.println("Created dummy EML file at: " + emlFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to create dummy EML file: " + e.getMessage());
                return;
            }
        }


        try {
            Properties props = new Properties();
            jakarta.mail.Session mailSession = jakarta.mail.Session.getDefaultInstance(props, null);
            InputStream source = new FileInputStream(emlFile);
            MimeMessage messageToForward = new MimeMessage(mailSession, source);
            source.close();

            // Extract recipient from the EML file's "To" header for the RCPT TO command
            // Or use a fixed recipient for testing with the dummy server
            String recipientEmail;
            if (messageToForward.getRecipients(jakarta.mail.Message.RecipientType.TO) != null &&
                messageToForward.getRecipients(jakarta.mail.Message.RecipientType.TO).length > 0) {
                recipientEmail = messageToForward.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString();
                // For local testing, override to ensure it goes to localhost
                if (recipientEmail.contains("@example.com")) { // Simple check
                    recipientEmail = recipientEmail.substring(0, recipientEmail.indexOf("@")) + "@localhost";
                }
                 if (!recipientEmail.endsWith("@localhost")){ // Ensure it's a localhost recipient for the dummy server
                    recipientEmail = "recipient@localhost"; // Default fallback if parsing is complex
                    System.out.println("Overriding recipient to: " + recipientEmail + " for local test server.");
                }

            } else {
                System.err.println("No 'To' recipient found in EML, using default 'recipient@localhost'");
                recipientEmail = "recipient@localhost";
            }
            
            // The MAIL FROM for the SMTP envelope will be sender.mailFrom ("sender-from-actual-send@example.com")
            // The From header inside the MimeMessage will be from the .eml file.

            System.out.println("Sending loaded .eml file to: " + recipientEmail);
            System.out.println("Subject: " + messageToForward.getSubject());
            
            boolean success = sender.sendEmail(recipientEmail, messageToForward);
            System.out.println("\nMain: Email send attempt result: " + success);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
