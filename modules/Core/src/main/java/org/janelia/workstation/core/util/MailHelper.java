package org.janelia.workstation.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

public class MailHelper {

    private static final Logger log = LoggerFactory.getLogger(MailHelper.class);

    private static final String DEFAULT_SMTP_PORT = "25";

    public MailHelper() {
    }

    public void sendEmail(String from, String to, String subject, String bodyText) {
        this.sendEmail(from, to, subject, bodyText, null, null);
    }

    public void sendEmail(String from, String to, String subject, String bodyText, File attachedFile, String filename) {
        try {
            String MAIL_SERVER = ConsoleProperties.getString("console.MailServer");
            String smtpUser = ConsoleProperties.getString("console.SMTPUser", "");
            String smtpPassword = ConsoleProperties.getString("console.SMTPPassword", "");
            String[] split = MAIL_SERVER.split(":");
            String host = split[0];
            String port = DEFAULT_SMTP_PORT;
            if (split.length > 1) {
                port = split[1];
            }

            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            Authenticator authenticator;
            if (smtpUser.trim().length() > 0 && smtpPassword.trim().length() > 0) {
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                authenticator = new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                smtpUser, smtpPassword);
                    }
                };
            } else {
                authenticator = null;
            }

            Session session = Session.getDefaultInstance(properties, authenticator);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("[JW] " + subject);
            BodyPart messagePart = new MimeBodyPart();
            messagePart.setText(bodyText);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            if (attachedFile != null) {
                if (filename == null) {
                    filename = attachedFile.getName();
                }

                BodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachedFile.getAbsolutePath())));
                attachmentPart.setFileName(filename);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);

            log.info("Sent email successfully");
            log.info("  From: " + from);
            log.info("  To: " + to);
            log.info("  Body: " + bodyText);

        }
        catch (MessagingException var13) {
            log.error("Error sending email", var13);
        }

    }
}
