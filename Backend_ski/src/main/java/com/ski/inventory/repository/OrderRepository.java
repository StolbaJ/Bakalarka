package com.ski.inventory.repository;

import com.ski.inventory.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT o
                    FROM Order o
                    LEFT JOIN o.customer c
                    WHERE (:search IS NULL OR :search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:taskName IS NULL OR :taskName = '' OR EXISTS (
                          SELECT 1
                          FROM OrderTask ot
                          JOIN ot.taskItems ti
                          WHERE ot.order = o
                            AND ti.taskName = :taskName
                      ))
                    ORDER BY o.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT o.id)
                    FROM Order o
                    LEFT JOIN o.customer c
                    WHERE (:search IS NULL OR :search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:taskName IS NULL OR :taskName = '' OR EXISTS (
                          SELECT 1
                          FROM OrderTask ot
                          JOIN ot.taskItems ti
                          WHERE ot.order = o
                            AND ti.taskName = :taskName
                      ))
                    """
    )
    Page<Order> findOrdersWithFilters(
            @Param("search") String search,
            @Param("taskName") String taskName,
            Pageable pageable
    );

    /** Pro cron: objednávky, u kterých ještě nebyl odeslán e-mail o založení. */
    List<Order> findByOrderCreatedEmailSentFalse();
}
