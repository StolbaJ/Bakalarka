package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.OrderTask;
import cz.cvut.fel.skiapp.backendconnection.model.ServiceTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
