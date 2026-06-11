package com.ski.inventory.service;

import com.ski.inventory.model.Customer;
import com.ski.inventory.model.Order;
import com.ski.inventory.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Odeslání e-mailu „objednávka založena“ zákazníkovi.
 * Používá se při vytvoření objednávky (ručně) i v cronu pro objednávky importované z jiného webu (bez lyží),
 * kde technik doplní lyže později – cron projde objednávky s {@code orderCreatedEmailSent = false} a e-mail doplní.
 */
@Service
public class OrderCreatedNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedNotificationService.class);

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OrderViewTokenService orderViewTokenService;
    private final String frontendBaseUrl;

    public OrderCreatedNotificationService(
            OrderRepository orderRepository,
            EmailService emailService,
            OrderViewTokenService orderViewTokenService,
            @Value("${app.frontend-base-url:https://bezkyservis.xyz}") String frontendBaseUrl) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.orderViewTokenService = orderViewTokenService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Odešle e-mail o založení objednávky zákazníkovi (pokud má e-mail) a nastaví {@code orderCreatedEmailSent = true}.
     * Volá se z controlleru po vytvoření objednávky i z cronu pro objednávky, kde e-mail ještě nešel.
     */
    @Transactional
    public void sendAndMarkSent(Order order) {
        Customer customer = order.getCustomer();
        if (customer == null) {
            return;
        }
        String email = customer.getEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        String orderNumber = order.getOrderNumber();
        if (orderNumber == null) {
            return;
        }
        String viewLink = null;
        String phone = customer.getPhone();
        if (phone != null && !phone.isBlank()) {
            String viewToken = orderViewTokenService.createToken(order.getId(), phone);
            viewLink = frontendBaseUrl.replaceAll("/$", "") + "/objednavka?token=" + URLEncoder.encode(viewToken, StandardCharsets.UTF_8);
        }
        emailService.sendOrderCreatedNotification(email, orderNumber, customer.getName(), viewLink);
        order.setOrderCreatedEmailSent(true);
        orderRepository.save(order);
        log.info("Order created email sent for order {} to {}", orderNumber, email);
    }

    /**
     * Cron: každých 5 minut projde objednávky, u kterých ještě nebyl odeslán e-mail o založení,
     * a odešle ho tam, kde zákazník má e-mail (např. objednávky importované z jiného webu).
     */
    @Scheduled(fixedDelayString = "${app.order-created-email-cron-interval-ms:300000}") // default 5 min
    @Transactional
    public void sendPendingOrderCreatedEmails() {
        List<Order> pending = orderRepository.findByOrderCreatedEmailSentFalse();
        for (Order order : pending) {
            try {
                if (order.getCustomer() != null && order.getCustomer().getEmail() != null && !order.getCustomer().getEmail().isBlank()) {
                    sendAndMarkSent(order);
                }
            } catch (Exception e) {
                log.warn("Failed to send order created email for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
