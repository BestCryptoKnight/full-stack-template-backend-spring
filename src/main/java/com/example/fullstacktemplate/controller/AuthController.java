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
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginRequest.getEmail(), loginRequest.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getEmailVerified()) {
            if (user.getTwoFactorEnabled()) {
                AuthResponse authResponse = new AuthResponse();
                authResponse.setTwoFactorRequired(true);
                return ResponseEntity.ok().body(authResponse);
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
                return ResponseEntity.ok(new TokenResponse(jwtTokenProvider.createTokenValue(optionalUser.get().getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS))));
            }
        }
        throw new BadRequestException("tokenExpired");
    }

    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyLogin(@Valid @RequestBody LoginVerificationRequest loginVerificationRequest, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginVerificationRequest.getEmail(), loginVerificationRequest.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (authenticationService.isVerificationCodeValid(userPrincipal.getId(), loginVerificationRequest.getCode())) {
            return ResponseEntity.ok(getAuthResponse(user, response));
        } else {
            throw new BadRequestException("invalidVerificationCode");
        }
    }

    @PostMapping("/login/recovery-code")
    public ResponseEntity<?> loginRecoveryCode(@Valid @RequestBody LoginVerificationRequest loginVerificationRequest, HttpServletResponse response) {
        Authentication authentication = getAuthentication(loginVerificationRequest.getEmail(), loginVerificationRequest.getPassword());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (authenticationService.isRecoveryCodeValid(userPrincipal.getId(), loginVerificationRequest.getCode())) {
            authenticationService.deleteRecoveryCode(user.getId(), loginVerificationRequest.getCode());
            return ResponseEntity.ok(getAuthResponse(user, response));
        } else {
            throw new BadRequestException("invalidRecoveryCode");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) throws URISyntaxException, IOException {
        if (userService.isEmailUsed(signUpRequest.getEmail())) {
            throw new BadRequestException("emailInUse");
        }
        if (userService.isUsernameUsed(signUpRequest.getName())) {
            throw new BadRequestException("usernameInUse");
        }
        User user = userService.createNewUser(signUpRequest);
        String tokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
        tokenRepository.save(userService.createToken(user, tokenValue, TokenType.ACCOUNT_ACTIVATION));
        emailService.sendAccountActivationMessage(signUpRequest, tokenValue);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location)
                .body(new ApiResponse(true, messageService.getMessage("userWasRegistered")));
    }

    @PostMapping("/activateAccount")
    public ResponseEntity<?> activateUserAccount(@Valid @RequestBody TokenAccessRequest tokenAccessRequest) {
        userService.activateUserAccount(tokenAccessRequest);
        return ResponseEntity.ok()
                .body(new ApiResponse(true, messageService.getMessage("accountActivated")));
    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<?> confirmEmailChange(@Valid @RequestBody TokenAccessRequest tokenAccessRequest) {
        userService.activateRequestedEmail(tokenAccessRequest);
        return ResponseEntity.ok()
                .body(new ApiResponse(true, messageService.getMessage("emailUpdated")));

    }

    @PostMapping("/forgottenPassword")
    public ResponseEntity<?> forgottenPassword(@Valid @RequestBody ForgottenPasswordRequest
                                                       forgottenPasswordRequest) throws MalformedURLException, URISyntaxException {
        User user = userService.findByEmail(forgottenPasswordRequest.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getEmailVerified()) {
            String tokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
            userService.createToken(user, tokenValue, TokenType.FORGOTTEN_PASSWORD);
            emailService.sendPasswordResetMessage(forgottenPasswordRequest, tokenValue);
            return ResponseEntity
                    .ok()
                    .body(new ApiResponse(true, messageService.getMessage("passwordResetEmailSentMessage")));
        } else {
            throw new BadRequestException("accountNotActivated");
        }
    }

    @PostMapping("/passwordReset")
    public ResponseEntity<?> passwordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        User user = userService.findByEmail(passwordResetRequest.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        Optional<JwtToken> optionalForgottenPassword = tokenRepository.findByUserAndTokenType(user, TokenType.FORGOTTEN_PASSWORD);
        if (!optionalForgottenPassword.isPresent() || !optionalForgottenPassword.get().getValue().equals(passwordResetRequest.getToken())) {
            throw new BadRequestException("invalidToken");
        } else if (!jwtTokenProvider.validateToken(passwordResetRequest.getToken())) {
            throw new BadRequestException("tokenExpired");
        } else {
            userService.updateUserPassword(user, passwordResetRequest.getPassword());
            tokenRepository.delete(optionalForgottenPassword.get());
            return ResponseEntity.ok()
                    .body(new ApiResponse(true, messageService.getMessage("passwordWasReset")));
        }
    }

    private AuthResponse getAuthResponse(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponse authResponse = new AuthResponse();
        authResponse.setTwoFactorRequired(user.getTwoFactorEnabled());
        authResponse.setAccessToken(accessToken);
        String refreshTokenValue = jwtTokenProvider.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getPersistentTokenExpirationMsec(), ChronoUnit.MILLIS));
        JwtToken refreshToken = tokenRepository.save(userService.createToken(user, refreshTokenValue, TokenType.REFRESH));
        response.setHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_NAME + "=" + refreshToken.getValue() + "; Path=/; HttpOnly; SameSite=Strict;");
        return authResponse;
    }

    private Authentication getAuthentication(String email, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }
}
