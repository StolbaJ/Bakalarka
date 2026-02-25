package com.ski.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Vždy poskytne bean JavaMailSender.
 * Když je mail vypnutý nebo nenakonfigurovaný, použije se no-op implementace,
 * aby aplikace (EmailService) mohla startovat i bez SMTP.
 */
@Configuration
public class MailConfig {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.host:}")
    private String host;

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        if (mailEnabled && host != null && !host.isBlank()) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            return sender;
        }
        return new NoOpJavaMailSender();
    }

    /** No-op implementace – při vypnutém mailu jen nic neodešle. */
    private static class NoOpJavaMailSender extends JavaMailSenderImpl {
        @Override
        public void send(SimpleMailMessage simpleMessage) {
            // nic
        }
        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            // nic
        }
        @Override
        public void send(MimeMessage mimeMessage) {
            // nic
        }
        @Override
        public void send(MimeMessage... mimeMessages) {
            // nic
        }
    }
}
