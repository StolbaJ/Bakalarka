package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private ServiceOrder order;

    @ManyToOne
    private Ski ski;

    private String taskName; // Např. "Broušení hran", "Voskování"
    private String taskInstruction; // Např. "87 stupňů"
    private BigDecimal price;

    /*@Enumerated(EnumType.STRING)
    private ItemStatus status; // WAITING, IN_WORK, DONE*/

    // proměnné pro pohodu
    private String productText; // ord:text
    private String productCode; // ord:code
    private BigDecimal quantity; // ord:quantity
    private BigDecimal unitPrice; // typ:unitPrice
    private String unit; // ord:unit

    // proměnné pro shoptet
    private String name;
    //private String productCode; // Klíčové pro určení typu servisu (např. SERVIS_BRUS)
    private Integer amount;
    private String remark; // Zde mohou být parametry pro Svecom
}