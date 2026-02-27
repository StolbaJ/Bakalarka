package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findAllByOrderByCreatedAtDesc();
}