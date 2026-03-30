package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
    Optional<User> findByEmployeeId(Long employeeId);
}
