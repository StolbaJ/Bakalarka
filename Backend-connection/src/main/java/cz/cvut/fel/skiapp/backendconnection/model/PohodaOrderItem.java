package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pohoda_order_item")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class PohodaOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private PohodaOrder order;

    private String productText; // ord:text
    private String productCode; // ord:code
    private BigDecimal quantity; // ord:quantity
    private BigDecimal unitPrice; // typ:unitPrice
    private String unit; // ord:unit

}
