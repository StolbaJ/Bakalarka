package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shoptet_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ShoptetOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shoptetId; // ID ze Shoptetu
    private String orderCode;
    private LocalDateTime date;
    private String totalPrice;
    private String  isPaid;
    private String customerEmail;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<ShoptetOrderItem> items;

}
