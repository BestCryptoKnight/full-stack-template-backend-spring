package com.example.fullstacktemplate.repository;

import com.example.fullstacktemplate.model.TwoFactorRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TwoFactoryRecoveryCodeRepository extends JpaRepository<TwoFactorRecoveryCode, Long> {


}
