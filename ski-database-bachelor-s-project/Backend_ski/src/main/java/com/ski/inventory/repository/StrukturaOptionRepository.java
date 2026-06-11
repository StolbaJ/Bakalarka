package com.ski.inventory.repository;

import com.ski.inventory.model.StrukturaOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StrukturaOptionRepository extends JpaRepository<StrukturaOption, Long> {

    List<StrukturaOption> findAllByOrderBySortOrderAscNameAsc();
}
