package com.queueless.repository;

import com.queueless.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query to find a user during the login process
    Optional<User> findByEmail(String email);
}