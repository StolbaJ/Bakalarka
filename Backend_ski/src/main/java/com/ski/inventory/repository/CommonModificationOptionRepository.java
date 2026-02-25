package com.ski.inventory.repository;

import com.ski.inventory.model.CommonModificationOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonModificationOptionRepository extends JpaRepository<CommonModificationOption, Long> {

    List<CommonModificationOption> findAllByOrderBySortOrderAscNameAsc();

    java.util.Optional<CommonModificationOption> findFirstByName(String name);
}
