package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.dto.mapper.UserMapper;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TwoFactorRecoveryCode;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.config.security.CurrentUser;
import com.example.fullstacktemplate.config.security.UserPrincipal;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
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
                .orElseThrow(() -> new BadRequestException("userNotFound"));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody UserDto userDto) throws MalformedURLException, URISyntaxException {
        userService.updateProfile(userPrincipal.getId(), userMapper.toEntity(userPrincipal.getId(), userDto));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel-account")
    public ResponseEntity<?> cancelAccount(@CurrentUser UserPrincipal userPrincipal) {
        userService.cancelUserAccount(userPrincipal.getId());
        return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("accountCancelled")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        user = userService.updatePassword(user, changePasswordDto);
        String accessToken = jwtTokenService.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponseDto authResponseDto = new AuthResponseDto();
        authResponseDto.setTwoFactorRequired(false);
        authResponseDto.setAccessToken(accessToken);
        authResponseDto.setMessage(messageService.getMessage("passwordUpdated"));
        return ResponseEntity.ok(authResponseDto);
    }

    @PutMapping("/disable-two-factor")
    public ResponseEntity<?> disableTwoFactor(@CurrentUser UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        userService.disableTwoFactorAuthentication(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/two-factor-setup")
    public ResponseEntity<?> getTwoFactorSetup(@CurrentUser UserPrincipal userPrincipal) throws QrGenerationException, MalformedURLException, URISyntaxException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
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
        TwoFactorResponseDto twoFactorResponseDto = new TwoFactorResponseDto();
        twoFactorResponseDto.setQrData(generator.generate(data));
        twoFactorResponseDto.setMimeType(generator.getImageMimeType());
        return ResponseEntity.ok().body(twoFactorResponseDto);
    }

    @PostMapping("/new-backup-codes")
    public ResponseEntity<?> getBackupCodes(@CurrentUser UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        twoFactoryRecoveryCodeRepository.deleteAll(user.getTwoFactorRecoveryCodes());
        TwoFactorVerificationResponseDto twoFactorVerificationResponseDto = new TwoFactorVerificationResponseDto();
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
        twoFactorVerificationResponseDto.setVerificationCodes(twoFactorRecoveryCodes.stream().map(TwoFactorRecoveryCode::getRecoveryCode).collect(Collectors.toList()));
        return ResponseEntity.ok().body(twoFactorVerificationResponseDto);
    }


    @PostMapping("/two-factor-setup-secret")
    public ResponseEntity<?> getTwoFactorSetupSecret(@CurrentUser UserPrincipal userPrincipal) throws MalformedURLException, URISyntaxException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
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
        return ResponseEntity.ok().body(new ApiResponseDto(true, messageService.getMessage("twoFactorSetupKeyWasSend")));
    }

    @PostMapping("/verify-two-factor")
    public ResponseEntity<?> verifyTwoFactor(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody TwoFactorVerificationRequestDto twoFactorVerificationRequestDto) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
        if (verifier.isValidCode(user.getTwoFactorSecret(), twoFactorVerificationRequestDto.getCode())) {
            user = userService.enableTwoFactorAuthentication(user);
            TwoFactorVerificationResponseDto twoFactorVerificationResponseDto = new TwoFactorVerificationResponseDto();
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
            twoFactorVerificationResponseDto.setVerificationCodes(twoFactorRecoveryCodes.stream().map(TwoFactorRecoveryCode::getRecoveryCode).collect(Collectors.toList()));
            return ResponseEntity.ok().body(twoFactorVerificationResponseDto);
        } else {
            throw new BadRequestException("invalidVerificationCode");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent() && optionalRefreshToken.get().getUser().getId().equals(user.getId())) {
            tokenRepository.delete(optionalRefreshToken.get());
            response.addCookie(userService.createEmptyRefreshTokenCookie());
            return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("loggedOut")));
        }
        throw new BadRequestException("tokenExpired");
    }
}
