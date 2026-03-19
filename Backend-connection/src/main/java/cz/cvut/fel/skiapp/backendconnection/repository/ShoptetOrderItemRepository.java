package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.ShoptetOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoptetOrderItemRepository extends JpaRepository<ShoptetOrderItem, Long> {
}
