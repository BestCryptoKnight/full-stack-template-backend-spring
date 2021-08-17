package com.example.springsocial.controller;

import com.example.springsocial.model.JwtToken;
import com.example.springsocial.model.TokenType;
import com.example.springsocial.model.TwoFactorRecoveryCode;
import com.example.springsocial.model.User;
import com.example.springsocial.payload.*;
import com.example.springsocial.repository.UserRepository;
import com.example.springsocial.security.CurrentUser;
import com.example.springsocial.security.UserPrincipal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class UserController extends Controller {


    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public User getCurrentUser(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/update-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody UpdateProfileRequest updateProfileRequest, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        if (user.getEmail().equals(updateProfileRequest.getEmail()) || !userRepository.existsByEmail(updateProfileRequest.getEmail())) {
            if (user.getName().equals(updateProfileRequest.getName()) || !userRepository.existsByName(updateProfileRequest.getName())) {
                if (!StringUtils.isEmpty(updateProfileRequest.getName())) {
                    user.setName(updateProfileRequest.getName());
                }
                if (!StringUtils.isEmpty(updateProfileRequest.getEmail()) && !user.getEmail().equals(updateProfileRequest.getEmail())) {
                    String tokenValue = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
                    tokenRepository.save(userService.createToken(user, tokenValue, TokenType.EMAIL_UPDATE));
                    emailService.sendEmailChangeConfirmationMessage(user, updateProfileRequest, tokenValue, localeResolver.resolveLocale(request));
                    user.setRequestedNewEmail(updateProfileRequest.getEmail());
                }
                Base64PayloadFile profileImage = updateProfileRequest.getProfileImage();
                if (profileImage != null && profileImage.getData() != null && !StringUtils.isEmpty(profileImage.getType())) {
                    user.getProfileImage().setData(Base64.getDecoder().decode(profileImage.getData()));
                    user.getProfileImage().setType(profileImage.getType());
                    fileRepository.save(user.getProfileImage());
                }
                UpdateProfileResponse updateProfileResponse = new UpdateProfileResponse();
                updateProfileResponse.setUser(userRepository.save(user));
                String message = messageSource.getMessage("userProfileUpdate", null, localeResolver.resolveLocale(request));
                if (!StringUtils.isEmpty(updateProfileRequest.getEmail()) && !user.getEmail().equals(updateProfileRequest.getEmail())) {
                    message += "." + messageSource.getMessage("confirmAccountEmailChangeMessage", null, localeResolver.resolveLocale(request));
                }
                updateProfileResponse.setMessage(message);
                return ResponseEntity.ok().body(updateProfileResponse);
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, messageSource.getMessage("usernameInUse", null, localeResolver.resolveLocale(request))));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse(false, messageSource.getMessage("emailInUse", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/cancel-account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> cancelAccount(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        userRepository.delete(user);
        return ResponseEntity.ok(new ApiResponse(true, messageSource.getMessage("accountCancelled", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/changePassword")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> changePassword(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody ChangePassword changePassword, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        if (passwordEncoder.matches(changePassword.getCurrentPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(changePassword.getNewPassword()));
            user = userRepository.save(user);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            changePassword.getNewPassword()
                    )
            );
            userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
            AuthResponse authResponse = new AuthResponse();
            authResponse.setTwoFactorRequired(false);
            authResponse.setAccessToken(token);
            authResponse.setMessage(messageSource.getMessage("passwordUpdated", null, localeResolver.resolveLocale(request)));
            return ResponseEntity.ok(authResponse);

        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, messageSource.getMessage("invalidCredentials", null, localeResolver.resolveLocale(request))));
        }
    }

    @PostMapping("/disable-two-factor")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> disableTwoFactor(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        user.getTwoFactorRecoveryCodes().clear();
        user = userRepository.save(user);
        return ResponseEntity.ok().body(new ApiResponse(true, messageSource.getMessage("twoFactorAuthenticationDisabled", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/getTwoFactorSetup")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTwoFactorSetup(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) throws QrGenerationException {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        user = userRepository.save(user);
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBackupCodes(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTwoFactorSetupSecret(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        emailService.sendSimpleMessage(
                user.getEmail(),
                messageSource.getMessage("twoFactorSetupEmailSubject", null, localeResolver.resolveLocale(request)),
                String.format("%s: %s. %s",
                        messageSource.getMessage("twoFactorSetupEmailBodyKeyIsPrefix", null, localeResolver.resolveLocale(request)),
                        user.getTwoFactorSecret(),
                        messageSource.getMessage("twoFactorSetupEmailBodyEnterKeyPrefix", null, localeResolver.resolveLocale(request)))
        );
        return ResponseEntity.ok().body(new ApiResponse(true, messageSource.getMessage("twoFactorSetupKeyWasSend", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/verifyTwoFactor")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> verifyTwoFactor(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody TwoFactorVerificationRequest twoFactorVerificationRequest, HttpServletRequest request) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();

        if (verifier.isValidCode(user.getTwoFactorSecret(), twoFactorVerificationRequest.getCode())) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            TwoFactorVerificationResponse twoFactorVerificationResponse = new TwoFactorVerificationResponse();
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
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidVerificationCode", null, localeResolver.resolveLocale(request))));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent() && optionalRefreshToken.get().getUser().getId().equals(user.getId())) {
            tokenRepository.delete(optionalRefreshToken.get());
            response.addCookie(userService.createEmptyRefreshTokenCookie());
            return ResponseEntity.ok(new ApiResponse(true, messageSource.getMessage("loggedOut", null, localeResolver.resolveLocale(request))));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
