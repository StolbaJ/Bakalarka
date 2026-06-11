package com.ski.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "common_modification_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonModificationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /** Pokud true, před označením úkonu jako dokončený musí být vyplněn popis práce (výsledek). */
    @Column(name = "requires_work_description", nullable = false)
    private boolean requiresWorkDescription = false;

    /** Cena úkonu dle ceníku (Kč). */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal price;
}
