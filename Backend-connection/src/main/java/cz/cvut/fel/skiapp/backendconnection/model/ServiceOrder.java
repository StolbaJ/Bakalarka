package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@Table(name = "service_orders")
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private OrderSource source; // SHOPTET, FRONTEND

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // QUEUED, SYNCED, IN_PROGRESS, COMPLETED, FAILED

    @Enumerated(EnumType.STRING)
    private OrderPriority priority;

    private LocalDateTime createdAt;
    private LocalDate dueDate;
    private String notes;

    // Integrační klíče (nullable)
    private String shoptetId;
    private String pohodaId;
    private String externalApiId; // ID z frontend API

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    // Údaje potřebné pro synchronizaci s Pohodou
    private String orderNumber; // typ:numberRequested
    private LocalDate dateCreated; // ord:date
    private LocalDate dateFrom; // ord:dateFrom
    private LocalDate dateTo; // ord:dateTo

    private String customerCompany; // typ:company
    private String customerName; // typ:name
    private String customerCity; // typ:city
    private String customerStreet; // typ:street
    private String customerZip; // typ:zip
    private String customerIco; // typ:ico
    private String customerEmail; // typ:email
    private String customerPhone; // typ:phone

    private String note; // ord:note
    private String internalNote; // ord:intNote
    private BigDecimal totalPrice; // typ:priceNone (v summary)

    // Údaje potřebné pro synchronizaci s Shoptetem

    //private String shoptetId; // ID ze Shoptetu
    private String orderCode;
    private LocalDateTime date;
    //private String totalPrice;
    private String  isPaid;
    //private String customerEmail;
}
