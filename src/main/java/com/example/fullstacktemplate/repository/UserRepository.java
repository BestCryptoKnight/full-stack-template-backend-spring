package com.example.fullstacktemplate.repository;

import com.example.fullstacktemplate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Boolean existsByEmail(String email);

    Boolean existsByName(String name);
}
