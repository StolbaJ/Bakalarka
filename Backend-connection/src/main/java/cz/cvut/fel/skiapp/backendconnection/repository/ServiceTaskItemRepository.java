package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.ServiceTaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTaskItemRepository extends JpaRepository<ServiceTaskItem, Long> {
}
