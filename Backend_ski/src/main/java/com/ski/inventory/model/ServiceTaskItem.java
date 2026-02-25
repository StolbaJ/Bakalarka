package com.ski.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_task_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTaskItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private OrderTask task;
    
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    /** Návod / jak úkon zpracovat (může být z úpravy při vytvoření). */
    @Column(name = "task_instruction", columnDefinition = "TEXT")
    private String taskInstruction;
    
    /** Výsledek / zakončovací popis (povinný pokud úprava má requires_work_description). */
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;
    
    @Column(nullable = false)
    private Boolean completed = false;

    /** Zda před dokončením úkonu musí být vyplněn popis práce (přebírá se z typu úpravy). */
    @Column(name = "requires_work_description", nullable = false)
    private Boolean requiresWorkDescription = false;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
