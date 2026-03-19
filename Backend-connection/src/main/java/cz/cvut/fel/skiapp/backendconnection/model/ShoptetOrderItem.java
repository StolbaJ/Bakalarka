package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@Entity
@Table(name = "shoptet_order_items")
public class ShoptetOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String productCode; // Klíčové pro určení typu servisu (např. SERVIS_BRUS)
    private Integer amount;
    private String remark; // Zde mohou být parametry pro Svecom

    @ManyToOne
    @JoinColumn(name = "order_id")
    private ShoptetOrder order;

    // Gettery a Settery...
}