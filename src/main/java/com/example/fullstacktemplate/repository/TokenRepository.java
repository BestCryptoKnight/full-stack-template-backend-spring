package com.example.fullstacktemplate.repository;

import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TokenType;
import com.example.fullstacktemplate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<JwtToken, Long> {


    Optional<JwtToken> findByUserAndTokenType(User user, TokenType tokenType);

    Optional<JwtToken> findByValueAndTokenType(String value, TokenType tokenType);

}
