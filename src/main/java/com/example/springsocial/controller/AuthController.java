package com.example.springsocial.controller;

import com.example.springsocial.model.JwtToken;
import com.example.springsocial.model.TokenType;
import com.example.springsocial.model.TwoFactorRecoveryCode;
import com.example.springsocial.model.User;
import com.example.springsocial.payload.*;
import com.example.springsocial.security.UserPrincipal;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.Cookie;
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

import static com.example.springsocial.service.UserService.REFRESH_TOKEN_COOKIE_NAME;

@RestController
@RequestMapping("/auth")
public class AuthController extends Controller {


    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = getAuthentication(loginRequest.getEmail(), loginRequest.getPassword());
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(userPrincipal.getEmail()).orElse(null);
            if (user != null) {
                if (user.getEmailVerified()) {
                    if (user.getTwoFactorEnabled()) {
                        AuthResponse authResponse = new AuthResponse();
                        authResponse.setTwoFactorRequired(true);
                        return ResponseEntity.ok().body(authResponse);
                    } else {
                        return ResponseEntity.ok(getAuthResponse(user, response));
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, messageSource.getMessage("accountNotActivated", null, localeResolver.resolveLocale(request))));
                }
            }
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, messageSource.getMessage("invalidCredentials", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAuth(HttpServletRequest request) {
        Optional<JwtToken> optionalRefreshToken = userService.getRefreshTokenFromRequest(request);
        if (optionalRefreshToken.isPresent()) {
            Optional<User> optionalUser = userRepository.findById(jwtTokenProvider.getUserIdFromToken(optionalRefreshToken.get().getValue()));
            if (optionalUser.isPresent() && optionalRefreshToken.get().getUser().getId().equals(optionalUser.get().getId())) {
                return ResponseEntity.ok(new TokenResponse(jwtTokenProvider.createToken(optionalUser.get(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS))));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, messageSource.getMessage("invalidAccess", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyLogin(@Valid @RequestBody LoginVerificationRequest
                                                 loginVerificationRequest, HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication authentication = getAuthentication(loginVerificationRequest.getEmail(), loginVerificationRequest.getPassword());
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
            TimeProvider timeProvider = new SystemTimeProvider();
            CodeGenerator codeGenerator = new DefaultCodeGenerator();
            CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

            if (verifier.isValidCode(user.getTwoFactorSecret(), loginVerificationRequest.getCode())) {
                return ResponseEntity.ok(getAuthResponse(user, response));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidVerificationCode", null, localeResolver.resolveLocale(request))));
            }
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, messageSource.getMessage("invalidCredentials", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/login/recovery-code")
    public ResponseEntity<?> loginRecoveryCode(@Valid @RequestBody LoginVerificationRequest
                                                       loginVerificationRequest, HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication authentication = getAuthentication(loginVerificationRequest.getEmail(), loginVerificationRequest.getPassword());
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
            Optional<TwoFactorRecoveryCode> optionalRecoveryCode = user.getTwoFactorRecoveryCodes()
                    .stream()
                    .filter(twoFactorRecoveryCode -> loginVerificationRequest.getCode().equals(twoFactorRecoveryCode.getRecoveryCode()))
                    .findFirst();
            if (optionalRecoveryCode.isPresent()) {
                twoFactoryRecoveryCodeRepository.delete(optionalRecoveryCode.get());
                return ResponseEntity.ok(getAuthResponse(user, response));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidRecoveryCode", null, localeResolver.resolveLocale(request))));
            }
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, messageSource.getMessage("invalidCredentials", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest, HttpServletRequest request) throws
            URISyntaxException, IOException {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, messageSource.getMessage("emailInUse", null, localeResolver.resolveLocale(request))));
        }
        if (userRepository.existsByName(signUpRequest.getName())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, messageSource.getMessage("usernameInUse", null, localeResolver.resolveLocale(request))));
        }
        User user = userService.createNewUser(signUpRequest);
        user = userRepository.save(user);
        String tokenValue = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
        tokenRepository.save(userService.createToken(user, tokenValue, TokenType.ACCOUNT_ACTIVATION));
        emailService.sendAccountActivationMessage(signUpRequest, tokenValue, localeResolver.resolveLocale(request));
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location)
                .body(new ApiResponse(true, messageSource.getMessage("userWasRegistered", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/activateAccount")
    public ResponseEntity<?> activateUserAccount(@Valid @RequestBody TokenAccessRequest tokenAccessRequest, HttpServletRequest request) {
        Optional<JwtToken> optionalVerificationToken = tokenRepository.findByValueAndTokenType(tokenAccessRequest.getToken(), TokenType.ACCOUNT_ACTIVATION);
        if (optionalVerificationToken.isPresent()) {
            User user = optionalVerificationToken.get().getUser();
            if (!jwtTokenProvider.validateToken(tokenAccessRequest.getToken())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("tokenExpired", null, localeResolver.resolveLocale(request))));
            } else {
                user.setEmailVerified(true);
                userRepository.save(user);
                tokenRepository.delete(optionalVerificationToken.get());
                return ResponseEntity.ok()
                        .body(new ApiResponse(true, messageSource.getMessage("accountActivated", null, localeResolver.resolveLocale(request))));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidToken", null, localeResolver.resolveLocale(request))));

    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<?> confirmEmailChange(@Valid @RequestBody TokenAccessRequest tokenAccessRequest, HttpServletRequest request) {
        Optional<JwtToken> optionalVerificationToken = tokenRepository.findByValueAndTokenType(tokenAccessRequest.getToken(), TokenType.EMAIL_UPDATE);
        if (optionalVerificationToken.isPresent()) {
            User user = optionalVerificationToken.get().getUser();
            if (!jwtTokenProvider.validateToken(tokenAccessRequest.getToken())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("tokenExpired", null, localeResolver.resolveLocale(request))));
            } else {
                user.setEmail(user.getRequestedNewEmail());
                user.setRequestedNewEmail(null);
                userRepository.save(user);
                tokenRepository.delete(optionalVerificationToken.get());
                return ResponseEntity.ok()
                        .body(new ApiResponse(true, messageSource.getMessage("emailUpdated", null, localeResolver.resolveLocale(request))));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidToken", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/forgottenPassword")
    public ResponseEntity<?> forgottenPassword(@Valid @RequestBody ForgottenPasswordRequest
                                                       forgottenPasswordRequest, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        Optional<User> optionalUser = userRepository.findByEmail(forgottenPasswordRequest.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getEmailVerified()) {
                String tokenValue = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
                tokenRepository.save(userService.createToken(user, tokenValue, TokenType.FORGOTTEN_PASSWORD));
                emailService.sendPasswordResetMessage(forgottenPasswordRequest, tokenValue, localeResolver.resolveLocale(request));
                return ResponseEntity
                        .ok()
                        .body(new ApiResponse(true, messageSource.getMessage("passwordResetEmailSentMessage", null, localeResolver.resolveLocale(request))));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, messageSource.getMessage("accountNotActivated", null, localeResolver.resolveLocale(request))));

            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, messageSource.getMessage("userWithEmailNotExist", null, localeResolver.resolveLocale(request))));
    }

    @PostMapping("/passwordReset")
    public ResponseEntity<?> passwordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest, HttpServletRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(passwordResetRequest.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            Optional<JwtToken> optionalForgottenPassword = tokenRepository.findByUserAndTokenType(user, TokenType.FORGOTTEN_PASSWORD);
            if (!optionalForgottenPassword.isPresent() || !optionalForgottenPassword.get().getValue().equals(passwordResetRequest.getToken())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("invalidToken", null, localeResolver.resolveLocale(request))));
            } else if (!jwtTokenProvider.validateToken(passwordResetRequest.getToken())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, messageSource.getMessage("tokenExpired", null, localeResolver.resolveLocale(request))));
            } else {
                userRepository.save(userService.updateUserPassword(user, passwordResetRequest.getPassword()));
                tokenRepository.delete(optionalForgottenPassword.get());
                return ResponseEntity.ok()
                        .body(new ApiResponse(true, messageSource.getMessage("passwordWasReset", null, localeResolver.resolveLocale(request))));
            }

        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, messageSource.getMessage("userWithEmailNotExist", null, localeResolver.resolveLocale(request))));
    }

    private AuthResponse getAuthResponse(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));
        AuthResponse authResponse = new AuthResponse();
        authResponse.setTwoFactorRequired(user.getTwoFactorEnabled());
        authResponse.setAccessToken(accessToken);
        String refreshTokenValue = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getPersistentTokenExpirationMsec(), ChronoUnit.MILLIS));
        JwtToken refreshToken = tokenRepository.save(userService.createToken(user, refreshTokenValue, TokenType.REFRESH));
        response.setHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_NAME+"="+refreshToken.getValue()+"; Path=/; HttpOnly; SameSite=Strict;");
        return authResponse;
    }

    private Authentication getAuthentication(String email, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }
}
