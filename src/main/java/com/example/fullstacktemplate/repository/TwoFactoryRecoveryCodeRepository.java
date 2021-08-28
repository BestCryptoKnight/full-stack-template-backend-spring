package com.example.fullstacktemplate.repository;

import com.example.fullstacktemplate.model.TwoFactorRecoveryCode;
import com.example.fullstacktemplate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TwoFactoryRecoveryCodeRepository extends JpaRepository<TwoFactorRecoveryCode, Long> {

   @Modifying
   @Query("DELETE from TwoFactorRecoveryCode t where t.userId=:userId and t.recoveryCode=:recoveryCode")
   public void deleteByUserIdAndRecoveryCode(@Param("userId") Long userId, @Param("recoveryCode") String recoveryCode);
}
