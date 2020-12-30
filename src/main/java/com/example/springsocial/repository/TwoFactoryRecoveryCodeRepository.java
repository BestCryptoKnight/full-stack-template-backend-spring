package com.example.springsocial.repository;

import com.example.springsocial.model.TwoFactorRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TwoFactoryRecoveryCodeRepository extends JpaRepository<TwoFactorRecoveryCode, Long> {


}
