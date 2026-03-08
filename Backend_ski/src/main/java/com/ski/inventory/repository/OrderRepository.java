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

    @Query("SELECT o FROM Order o LEFT JOIN o.customer c WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY o.createdAt DESC")
    Page<Order> searchByOrderNumberOrCustomerNameOrderByCreatedAtDesc(@Param("search") String search, Pageable pageable);

    /** Pro cron: objednávky, u kterých ještě nebyl odeslán e-mail o založení. */
    List<Order> findByOrderCreatedEmailSentFalse();
}
