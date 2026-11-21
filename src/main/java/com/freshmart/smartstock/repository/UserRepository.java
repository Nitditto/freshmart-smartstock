package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** One cheap indexed lookup per request: lets the JWT filter reject tokens of locked or deleted users. */
    boolean existsByIdAndActiveTrue(Long id);
}
