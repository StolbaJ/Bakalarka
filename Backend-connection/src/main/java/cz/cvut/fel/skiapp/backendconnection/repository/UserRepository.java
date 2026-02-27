package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.User;
import cz.cvut.fel.skiapp.backendconnection.model.UserRole;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndActiveTrue(String username);
    boolean existsByUsername(String username);
    long countByRole(UserRole role);
}
