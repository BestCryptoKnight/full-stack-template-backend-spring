package com.example.springsocial.repository;

import com.example.springsocial.model.JwtToken;
import com.example.springsocial.model.TokenType;
import com.example.springsocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<JwtToken, Long> {


    Optional<JwtToken> findByUserAndTokenType(User user, TokenType tokenType);

    Optional<JwtToken> findByValueAndTokenType(String value, TokenType tokenType);

}
