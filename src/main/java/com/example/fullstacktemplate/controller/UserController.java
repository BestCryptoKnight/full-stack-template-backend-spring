package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.exception.UserNotFoundException;
import com.example.fullstacktemplate.mapper.UserMapper;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TwoFactorRecoveryCode;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.security.CurrentUser;
import com.example.fullstacktemplate.security.UserPrincipal;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class UserController extends Controller {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/user/me")
    public UserDto getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        return userService.findById(userPrincipal.getId())
                .map(userMapper::toDto)
                .orElseThrow(UserNotFoundException::new);
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody UserDto userDto) throws MalformedURLException, URISyntaxException {
        userService.updateProfile(userPrincipal.getId(), userMapper.toEntity(userPrincipal.getId(), userDto));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel-account")
    public ResponseEntity<?> cancelAccount(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        userService.cancelUserAccount(userPrincipal.getId());
        return ResponseEntity.ok(new ApiResponse(true, messageService.getMessage("accountCancelled")));
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        user = userService.updatePassword(user, changePasswordDto);
        String accessToken = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponse authResponse = new AuthResponse();
        authResponse.setTwoFactorRequired(false);
        authResponse.setAccessToken(accessToken);
        authResponse.setMessage(messageService.getMessage("passwordUpdated"));
        return ResponseEntity.ok(authResponse);
    }

    @PutMapping("/disable-two-factor")
    public ResponseEntity<?> disableTwoFactor(@CurrentUser UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        userService.disableTwoFactorAuthentication(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/getTwoFactorSetup")
    public ResponseEntity<?> getTwoFactorSetup(@CurrentUser UserPrincipal userPrincipal) throws QrGenerationException, MalformedURLException, URISyntaxException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageService.getMessage("userNotFound")));
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        userService.updateProfile(userPrincipal.getId(), user);
        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(user.getTwoFactorSecret())
                .issuer(appProperties.getAppName())
                .algorithm(HashingAlgorithm.SHA512)
                .digits(6)
                .period(30)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        TwoFactorResponse twoFactorResponse = new TwoFactorResponse();
        twoFactorResponse.setQrData(generator.generate(data));
        twoFactorResponse.setMimeType(generator.getImageMimeType());
        return ResponseEntity.ok().body(twoFactorResponse);
    }

    @PostMapping("/getNewBackupCodes")
    public ResponseEntity<?> getBackupCodes(@CurrentUser UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageService.getMessage("userNotFound")));
        twoFactoryRecoveryCodeRepository.deleteAll(user.getTwoFactorRecoveryCodes());
        TwoFactorVerificationResponse twoFactorVerificationResponse = new TwoFactorVerificationResponse();
        RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
        List<TwoFactorRecoveryCode> twoFactorRecoveryCodes = Arrays.asList(recoveryCodeGenerator.generateCodes(16))
                .stream()
                .map(recoveryCode -> {
                    TwoFactorRecoveryCode twoFactorRecoveryCode = new TwoFactorRecoveryCode();
                    twoFactorRecoveryCode.setRecoveryCode(recoveryCode);
                    twoFactorRecoveryCode.setUser(user);
                    return twoFactorRecoveryCode;
                })
                .collect(Collectors.toList());
        twoFactorRecoveryCodes = twoFactoryRecoveryCodeRepository.saveAll(twoFactorRecoveryCodes);
        twoFactorVerificationResponse.setVerificationCodes(twoFactorRecoveryCodes.stream().map(TwoFactorRecoveryCode::getRecoveryCode).collect(Collectors.toList()));
        return ResponseEntity.ok().body(twoFactorVerificationResponse);
    }


    @PostMapping("/getTwoFactorSetupSecret")
    public ResponseEntity<?> getTwoFactorSetupSecret(@CurrentUser UserPrincipal userPrincipal) throws MalformedURLException, URISyntaxException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        userService.updateProfile(userPrincipal.getId(), user);
        emailService.sendSimpleMessage(
                user.getEmail(),
                messageService.getMessage("twoFactorSetupEmailSubject"),
                String.format("%s: %s. %s",
                        messageService.getMessage("twoFactorSetupEmailBodyKeyIsPrefix"),
                        user.getTwoFactorSecret(),
                        messageService.getMessage("twoFactorSetupEmailBodyEnterKeyPrefix"))
        );
        return ResponseEntity.ok().body(new ApiResponse(true, messageService.getMessage("twoFactorSetupKeyWasSend")));
    }

    @PostMapping("/verifyTwoFactor")
    public ResponseEntity<?> verifyTwoFactor(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody TwoFactorVerificationRequest twoFactorVerificationRequest) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
        if (verifier.isValidCode(user.getTwoFactorSecret(), twoFactorVerificationRequest.getCode())) {
            user = userService.enableTwoFactorAuthentication(user);
            TwoFactorVerificationResponse twoFactorVerificationResponse = new TwoFactorVerificationResponse();
            User finalUser = user;
            List<TwoFactorRecoveryCode> twoFactorRecoveryCodes = Arrays.asList(recoveryCodeGenerator.generateCodes(16))
                    .stream()
                    .map(recoveryCode -> {
                        TwoFactorRecoveryCode twoFactorRecoveryCode = new TwoFactorRecoveryCode();
                        twoFactorRecoveryCode.setRecoveryCode(recoveryCode);
                        twoFactorRecoveryCode.setUser(finalUser);
                        return twoFactorRecoveryCode;
                    })
                    .collect(Collectors.toList());
            twoFactorRecoveryCodes = twoFactoryRecoveryCodeRepository.saveAll(twoFactorRecoveryCodes);
            twoFactorVerificationResponse.setVerificationCodes(twoFactorRecoveryCodes.stream().map(TwoFactorRecoveryCode::getRecoveryCode).collect(Collectors.toList()));
            return ResponseEntity.ok().body(twoFactorVerificationResponse);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageService.getMessage("invalidVerificationCode")));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent() && optionalRefreshToken.get().getUser().getId().equals(user.getId())) {
            tokenRepository.delete(optionalRefreshToken.get());
            response.addCookie(userService.createEmptyRefreshTokenCookie());
            return ResponseEntity.ok(new ApiResponse(true, messageService.getMessage("loggedOut")));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
