package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TokenType;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.example.fullstacktemplate.service.UserService.REFRESH_TOKEN_COOKIE_NAME;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController extends Controller {


    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getEmailVerified()) {
            if (user.getTwoFactorEnabled()) {
                AuthResponseDto authResponseDto = new AuthResponseDto();
                authResponseDto.setTwoFactorRequired(true);
                return ResponseEntity.ok().body(authResponseDto);
            } else {
                return ResponseEntity.ok(getAuthResponse(user, response));
            }
        } else {
            throw new BadRequestException("accountNotActivated");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAuth(HttpServletRequest request) {
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent()) {
            Optional<User> optionalUser = userService.findById(jwtTokenProvider.getUserIdFromToken(optionalRefreshToken.get().getValue()));
            if (optionalUser.isPresent() && optionalRefreshToken.get().getUser().getId().equals(optionalUser.get().getId())) {
                return ResponseEntity.ok(new TokenResponseDto(jwtTokenProvider.createTokenValue(optionalUser.get().getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS))));
            }
        }
        throw new BadRequestException("tokenExpired");
    }

    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyLogin(@Valid @RequestBody LoginVerificationRequestDto loginVerificationRequestDto, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginVerificationRequestDto.getEmail(), loginVerificationRequestDto.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (authenticationService.isVerificationCodeValid(userPrincipal.getId(), loginVerificationRequestDto.getCode())) {
            return ResponseEntity.ok(getAuthResponse(user, response));
        } else {
            throw new BadRequestException("invalidVerificationCode");
        }
    }

    @PostMapping("/login/recovery-code")
    public ResponseEntity<?> loginRecoveryCode(@Valid @RequestBody LoginVerificationRequestDto loginVerificationRequestDto, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginVerificationRequestDto.getEmail(), loginVerificationRequestDto.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (authenticationService.isRecoveryCodeValid(userPrincipal.getId(), loginVerificationRequestDto.getCode())) {
            authenticationService.deleteRecoveryCode(user.getId(), loginVerificationRequestDto.getCode());
            return ResponseEntity.ok(getAuthResponse(user, response));
        } else {
            throw new BadRequestException("invalidRecoveryCode");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) throws URISyntaxException, IOException {
        if (userService.isEmailUsed(signUpRequestDto.getEmail())) {
            throw new BadRequestException("emailInUse");
        }
        if (userService.isUsernameUsed(signUpRequestDto.getName())) {
            throw new BadRequestException("usernameInUse");
        }
        User user = userService.createNewUser(signUpRequestDto);
        String tokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
        tokenRepository.save(userService.createToken(user, tokenValue, TokenType.ACCOUNT_ACTIVATION));
        emailService.sendAccountActivationMessage(signUpRequestDto, tokenValue);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location)
                .body(new ApiResponseDto(true, messageService.getMessage("userWasRegistered")));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<?> activateUserAccount(@Valid @RequestBody TokenAccessRequestDto tokenAccessRequestDto) {
        userService.activateUserAccount(tokenAccessRequestDto);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(true, messageService.getMessage("accountActivated")));
    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<?> confirmEmailChange(@Valid @RequestBody TokenAccessRequestDto tokenAccessRequestDto) {
        userService.activateRequestedEmail(tokenAccessRequestDto);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(true, messageService.getMessage("emailUpdated")));

    }

    @PostMapping("/forgotten-password")
    public ResponseEntity<?> forgottenPassword(@Valid @RequestBody ForgottenPasswordRequestDto
                                                       forgottenPasswordRequestDto) throws MalformedURLException, URISyntaxException {
        User user = userService.findByEmail(forgottenPasswordRequestDto.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getEmailVerified()) {
            String tokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
            userService.createToken(user, tokenValue, TokenType.FORGOTTEN_PASSWORD);
            emailService.sendPasswordResetMessage(forgottenPasswordRequestDto, tokenValue);
            return ResponseEntity
                    .ok()
                    .body(new ApiResponseDto(true, messageService.getMessage("passwordResetEmailSentMessage")));
        } else {
            throw new BadRequestException("accountNotActivated");
        }
    }

    @PostMapping("/password-reset")
    public ResponseEntity<?> passwordReset(@Valid @RequestBody PasswordResetRequestDto passwordResetRequestDto) {
        User user = userService.findByEmail(passwordResetRequestDto.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        Optional<JwtToken> optionalForgottenPassword = tokenRepository.findByUserAndTokenType(user, TokenType.FORGOTTEN_PASSWORD);
        if (!optionalForgottenPassword.isPresent() || !optionalForgottenPassword.get().getValue().equals(passwordResetRequestDto.getToken())) {
            throw new BadRequestException("invalidToken");
        } else if (!jwtTokenProvider.validateToken(passwordResetRequestDto.getToken())) {
            throw new BadRequestException("tokenExpired");
        } else {
            userService.updateUserPassword(user, passwordResetRequestDto.getPassword());
            tokenRepository.delete(optionalForgottenPassword.get());
            return ResponseEntity.ok()
                    .body(new ApiResponseDto(true, messageService.getMessage("passwordWasReset")));
        }
    }

    private AuthResponseDto getAuthResponse(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponseDto authResponseDto = new AuthResponseDto();
        authResponseDto.setTwoFactorRequired(user.getTwoFactorEnabled());
        authResponseDto.setAccessToken(accessToken);
        String refreshTokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getPersistentTokenExpirationMsec(), ChronoUnit.MILLIS));
        JwtToken refreshToken = tokenRepository.save(userService.createToken(user, refreshTokenValue, TokenType.REFRESH));
        response.setHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_NAME + "=" + refreshToken.getValue() + "; Path=/; HttpOnly; SameSite=Strict;");
        return authResponseDto;
    }

    private Authentication getAuthentication(String email, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }
}
