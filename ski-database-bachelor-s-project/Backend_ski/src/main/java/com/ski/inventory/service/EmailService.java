package com.ski.inventory.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service pro odesílání e-mailů přes Resend API.
 * Pokud není mail zapnutý nebo chybí RESEND_API_KEY, pouze loguje obsah.
 * Odesílatel: no-reply@bezkyservis.xyz (nastavitelné přes app.mail.from).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final boolean mailEnabled;
    private final String fromAddress;
    private final Resend resend;

    public EmailService(
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:BezkyServis <no-reply@bezkyservis.xyz>}") String fromAddress,
            @Value("${RESEND_API_KEY:}") String resendApiKey) {
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress != null ? fromAddress.trim() : "BezkyServis <no-reply@bezkyservis.xyz>";
        this.resend = (mailEnabled && resendApiKey != null && !resendApiKey.isBlank())
                ? new Resend(resendApiKey.trim())
                : null;
    }

    /**
     * Odešle e-mail s přihlašovacími údaji novému uživateli.
     */
    public void sendNewUserCredentials(String toEmail, String username, String password, String fullName) {
        String subject = "Přihlašovací údaje - BezkyServis";
        String body = buildCredentialsEmailBody(username, password, fullName);
        sendEmail(toEmail, subject, body);
    }

    /**
     * Odešle e-mail s novým heslem po resetu.
     */
    public void sendPasswordReset(String toEmail, String username, String newPassword, String fullName) {
        String subject = "Nové heslo - BezkyServis";
        String body = "Dobrý den" + (fullName != null && !fullName.isBlank() ? " " + fullName : "") + ",\n\n"
                + "Vaše heslo bylo resetováno.\n\n"
                + "Uživatelské jméno: " + username + "\n"
                + "Nové heslo: " + newPassword + "\n\n"
                + "Doporučujeme heslo po prvním přihlášení změnit.\n\n"
                + "S pozdravem,\nBezkyServis";
        sendEmail(toEmail, subject, body);
    }

    /**
     * Odešle zákazníkovi e-mail po vytvoření objednávky: číslo objednávky, info o sledování na webu, případně magický odkaz.
     * @param viewOrderLink může být null – pak se v e-mailu uvede jen návod na zadání čísla objednávky a telefonu
     */
    public void sendOrderCreatedNotification(String toEmail, String orderNumber, String customerName, String viewOrderLink) {
        String subject = "Objednávka " + orderNumber + " byla založena";
        String body = "Dobrý den" + (customerName != null && !customerName.isBlank() ? " " + customerName : "") + ",\n\n"
                + "Vaše objednávka byla založena.\n\n"
                + "Číslo objednávky: " + orderNumber + "\n\n"
                + "Stav objednávky můžete sledovat na webu bezkyservis.xyz po zadání čísla objednávky a telefonního čísla.";
        if (viewOrderLink != null && !viewOrderLink.isBlank()) {
            body += "\n\nPro přímý přístup k přehledu objednávky použijte tento odkaz:\n\n" + viewOrderLink;
        }
        body += "\n\nS pozdravem,\nBezkyServis";
        sendEmail(toEmail, subject, body);
    }

    /**
     * Odešle zákazníkovi e-mail, že jeho objednávka byla dokončena.
     */
    public void sendOrderReadyNotification(String toEmail, String orderNumber, String customerName) {
        String subject = "Objednávka " + orderNumber + " je připravena k vyzvednutí";
        String body = "Dobrý den" + (customerName != null && !customerName.isBlank() ? " " + customerName : "") + ",\n\n"
                + "Vaše lyže z objednávky " + orderNumber + " jsou připraveny k vyzvednutí.\n\n"
                + "Těšíme se na vás.\n\n"
                + "S pozdravem,\nBezkyServis";
        sendEmail(toEmail, subject, body);
    }

    private String buildCredentialsEmailBody(String username, String password, String fullName) {
        return "Dobrý den" + (fullName != null && !fullName.isBlank() ? " " + fullName : "") + ",\n\n"
                + "Byl vám vytvořen účet v systému BezkyServis.\n\n"
                + "Uživatelské jméno: " + username + "\n"
                + "Heslo: " + password + "\n\n"
                + "Přihlásit se můžete na webu: https://bezkyservis.xyz\n\n"
                + "Doporučujeme heslo po prvním přihlášení změnit.\n\n"
                + "S pozdravem,\nBezkyServis";
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailEnabled && resend != null) {
            try {
                CreateEmailOptions params = CreateEmailOptions.builder()
                        .from(fromAddress)
                        .to(to)
                        .subject(subject)
                        .text(body)
                        .build();
                resend.emails().send(params);
                log.info("E-mail odeslán na {}", to);
            } catch (ResendException e) {
                log.error("Chyba při odesílání e-mailu na {}: {}", to, e.getMessage());
                throw new RuntimeException("Nepodařilo se odeslat e-mail: " + e.getMessage());
            }
        } else {
            log.info("E-mail (simulace - mail není zapnut nebo chybí RESEND_API_KEY): To={}, Subject={}\nBody:\n{}", to, subject, body);
        }
    }
}
