package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.exception.UserNotFoundException;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.TwoFactoryRecoveryCodeRepository;
import com.example.fullstacktemplate.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationService {


    private final UserRepository userRepository;
    private final TwoFactoryRecoveryCodeRepository twoFactoryRecoveryCodeRepository;

    public AuthenticationService(UserRepository userRepository, TwoFactoryRecoveryCodeRepository twoFactoryRecoveryCodeRepository) {
        this.userRepository = userRepository;
        this.twoFactoryRecoveryCodeRepository = twoFactoryRecoveryCodeRepository;
    }

    public boolean isVerificationCodeValid(Long userId, String verificationCode) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(user.getTwoFactorSecret(), verificationCode);
    }

    public boolean isRecoveryCodeValid(Long userId, String recoveryCode) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return user.getTwoFactorRecoveryCodes()
                .stream()
                .anyMatch(twoFactorRecoveryCode -> recoveryCode.equals(twoFactorRecoveryCode.getRecoveryCode()));
    }

    public void deleteRecoveryCode(Long userId, String recoveryCode){
        twoFactoryRecoveryCodeRepository.deleteByUserIdAndRecoveryCode(userId,recoveryCode);
    }

}
