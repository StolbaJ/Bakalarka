package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "skis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ski {

    public Ski(String notes) {
        this.brand = "PROVIZORNI_LYZE";
        this.model = "PROVIZORNI_MODEL";
        this.length = "150";
        this.status = SkiStatus.PROVIZORNI;
        this.notes = notes;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Generated(event = EventType.INSERT)
    @Column(name = "ski_number") /*, unique = true, nullable = false, length = 12, insertable = false, updatable = false*/
    private String skiNumber;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 10)
    private String length;

    private Integer year;

    @Column(name = "ski_type", length = 50)
    private String skiType;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private java.math.BigDecimal weightKg;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "ski_condition")
    private SkiCondition condition = SkiCondition.DOBRY;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "ski_status")
    private SkiStatus status = SkiStatus.DOSTUPNY;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(length = 100)
    private String struktura;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
