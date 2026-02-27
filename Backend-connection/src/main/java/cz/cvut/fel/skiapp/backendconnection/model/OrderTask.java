package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ski_id", nullable = false)
    private Ski ski;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_task_status")
    private ServiceTaskStatus status = ServiceTaskStatus.CEKA;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_task_priority")
    private ServiceTaskPriority priority = ServiceTaskPriority.STREDNI;

    @Column(name = "estimated_time_hours", precision = 5, scale = 2)
    private BigDecimal estimatedTimeHours;

    @Column(name = "actual_time_hours", precision = 5, scale = 2)
    private BigDecimal actualTimeHours;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "target_struktura", length = 100)
    private String targetStruktura;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceTaskItem> taskItems = new ArrayList<>();
}
