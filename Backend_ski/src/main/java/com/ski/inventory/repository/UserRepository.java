package com.ski.inventory.repository;

import com.ski.inventory.model.User;
import com.ski.inventory.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndActiveTrue(String username);
    boolean existsByUsername(String username);
    long countByRole(UserRole role);
}
