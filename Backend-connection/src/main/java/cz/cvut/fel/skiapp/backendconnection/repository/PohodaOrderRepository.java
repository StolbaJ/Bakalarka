package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.PohodaOrder;
import org.springframework.stereotype.Repository;

@Repository
public interface PohodaOrderRepository {
    void save(PohodaOrder order);
}
