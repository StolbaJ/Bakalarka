package com.ski.inventory.repository;

import com.ski.inventory.model.OrderTask;
import com.ski.inventory.model.ServiceTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderTaskRepository extends JpaRepository<OrderTask, Long> {
    List<OrderTask> findByOrderId(Long orderId);

    long countByOrderId(Long orderId);

    long countByOrderIdAndStatus(Long orderId, ServiceTaskStatus status);

    @Query("SELECT t FROM OrderTask t LEFT JOIN FETCH t.ski LEFT JOIN FETCH t.taskItems WHERE t.order.id = :orderId")
    List<OrderTask> findByOrderIdWithSkiAndItems(@Param("orderId") Long orderId);

    List<OrderTask> findBySkiId(Long skiId);

    @Query("SELECT t FROM OrderTask t LEFT JOIN FETCH t.taskItems WHERE t.ski.id = :skiId ORDER BY t.createdAt DESC")
    List<OrderTask> findBySkiIdWithTaskItems(@Param("skiId") Long skiId);

    List<OrderTask> findByStatus(ServiceTaskStatus status);

    /** Počet tasků se statusem, které byly vytvořeny v daném období (created_at mezi from a to). */
    long countByStatusAndCreatedAtBetween(ServiceTaskStatus status, LocalDateTime from, LocalDateTime to);

    /** Průměrný čas dokončení v hodinách: pouze tasky s completed_at v období; průměr(completed_at - created_at). */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) / 3600.0) FROM order_tasks WHERE status = 'DOKONCENO' AND completed_at IS NOT NULL AND completed_at BETWEEN :from AND :to", nativeQuery = true)
    Double averageCompletionHoursBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Počet tasků vytvořených po dnech (created_at v období). Vrací [datum, počet]. */
    @Query(value = "SELECT CAST(created_at AS date), COUNT(*) FROM order_tasks WHERE created_at BETWEEN :from AND :to GROUP BY CAST(created_at AS date)", nativeQuery = true)
    List<Object[]> countCreatedByDayBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Počet tasků dokončených po dnech (completed_at v období). Vrací [datum, počet]. */
    @Query(value = "SELECT CAST(completed_at AS date), COUNT(*) FROM order_tasks WHERE status = 'DOKONCENO' AND completed_at IS NOT NULL AND completed_at BETWEEN :from AND :to GROUP BY CAST(completed_at AS date)", nativeQuery = true)
    List<Object[]> countCompletedByDayBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Nejčastější cílové struktury (target_struktura) v období. Vrací [struktura, počet]. Null a prázdný řetězec vynechány. */
    @Query("SELECT t.targetStruktura, COUNT(t) FROM OrderTask t WHERE t.createdAt BETWEEN :from AND :to AND t.targetStruktura IS NOT NULL AND t.targetStruktura <> '' GROUP BY t.targetStruktura ORDER BY COUNT(t) DESC")
    List<Object[]> countByTargetStrukturaAndCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
