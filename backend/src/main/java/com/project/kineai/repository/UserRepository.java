package com.project.kineai.repository;


import com.project.kineai.model.entity.User;
import com.project.kineai.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndActiveTrue(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
}
