package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pohoda_order")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Builder
public class PohodaOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pohodaId; // ord:id
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PohodaOrderItem> items = new ArrayList<>();

}
