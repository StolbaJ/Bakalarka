package com.ski.inventory.repository;

import com.ski.inventory.model.ServiceTaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceTaskItemRepository extends JpaRepository<ServiceTaskItem, Long> {

    /** Nejčastější názvy úkonů (task_name) v období – úkony z tasků vytvořených v intervalu. Vrací [název, počet]. */
    @Query("SELECT i.taskName, COUNT(i) FROM ServiceTaskItem i WHERE i.task.createdAt BETWEEN :from AND :to GROUP BY i.taskName ORDER BY COUNT(i) DESC")
    List<Object[]> countByTaskNameAndTaskCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
