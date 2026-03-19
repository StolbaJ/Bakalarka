package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.ShoptetOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoptetOrderRepository extends JpaRepository<ShoptetOrder, Long> {
    boolean existsByShoptetId(String attr0);
}
