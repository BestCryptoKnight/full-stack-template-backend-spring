package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.exception.EmailInUseException;
import com.example.fullstacktemplate.exception.UserNotFoundException;
import com.example.fullstacktemplate.exception.UsernameInUse;
import com.example.fullstacktemplate.mapper.UserMapper;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TokenType;
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
    public UserDto getCurrentUser(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        return userService.findById(userPrincipal.getId())
                .map(userMapper::toDto)
                .orElseThrow(UserNotFoundException::new);
    }

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody UserDto userDto, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (!userDto.getEmail().equals(userPrincipal.getEmail()) && userService.isEmailUsed(userDto.getEmail())) {
            throw new EmailInUseException();
        }
        if (!userDto.getName().equals(userPrincipal.getName()) && userService.isUsernameUsed(userDto.getName())) {
            throw new UsernameInUse();
        }

        String newEmail = userDto.getEmail();
        User user = userService.save(userMapper.toEntity(userPrincipal.getId(), userDto));
        if (!StringUtils.isEmpty(userDto.getEmail()) && !user.getEmail().equals(userDto.getEmail())) {
            String tokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
            userService.createToken(user, tokenValue, TokenType.EMAIL_UPDATE);
            emailService.sendEmailChangeConfirmationMessage(newEmail,user.getEmail(), tokenValue, localeResolver.resolveLocale(request));
        }

        UpdateProfileResponse updateProfileResponse = new UpdateProfileResponse();
        updateProfileResponse.setUser(userMapper.toDto(user));
        String message = messageSource.getMessage("userProfileUpdate", null, localeResolver.resolveLocale(request));
        if (!StringUtils.isEmpty(userDto.getEmail()) && !user.getEmail().equals(userDto.getEmail())) {
            message += "." + messageSource.getMessage("confirmAccountEmailChangeMessage", null, localeResolver.resolveLocale(request));
        }
        updateProfileResponse.setMessage(message);
        return ResponseEntity.ok().body(updateProfileResponse);
    }

    @PostMapping("/cancel-account")
    public ResponseEntity<?> cancelAccount(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        userService.cancelUserAccount(userPrincipal.getId());
        return ResponseEntity.ok(new ApiResponse(true, messageSource.getMessage("accountCancelled", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        user = userService.updatePassword(user, changePasswordDto);
        String accessToken = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponse authResponse = new AuthResponse();
        authResponse.setTwoFactorRequired(false);
        authResponse.setAccessToken(accessToken);
        authResponse.setMessage(messageSource.getMessage("passwordUpdated", null, localeResolver.resolveLocale(request)));
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/disable-two-factor")
    public ResponseEntity<?> disableTwoFactor(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        userService.disableTwoFactorAuthentication(user);
        return ResponseEntity.ok().body(new ApiResponse(true, messageSource.getMessage("twoFactorAuthenticationDisabled", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/getTwoFactorSetup")
    public ResponseEntity<?> getTwoFactorSetup(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) throws QrGenerationException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        user = userService.enableTwoFactorAuthentication(user);
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
    public ResponseEntity<?> getBackupCodes(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
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
    public ResponseEntity<?> getTwoFactorSetupSecret(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
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
    public ResponseEntity<?> verifyTwoFactor(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody TwoFactorVerificationRequest twoFactorVerificationRequest, HttpServletRequest request) {
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidVerificationCode", null, localeResolver.resolveLocale(request))));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(UserNotFoundException::new);
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent() && optionalRefreshToken.get().getUser().getId().equals(user.getId())) {
            tokenRepository.delete(optionalRefreshToken.get());
            response.addCookie(userService.createEmptyRefreshTokenCookie());
            return ResponseEntity.ok(new ApiResponse(true, messageSource.getMessage("loggedOut", null, localeResolver.resolveLocale(request))));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
