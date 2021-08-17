package com.example.springsocial.service;

import com.example.springsocial.config.AppProperties;
import com.example.springsocial.model.AuthProvider;
import com.example.springsocial.model.JwtToken;
import com.example.springsocial.model.TokenType;
import com.example.springsocial.model.User;
import com.example.springsocial.payload.SignUpRequest;
import com.example.springsocial.repository.TokenRepository;
import com.example.springsocial.security.JwtTokenProvider;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Service
public class UserService {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "rt_cookie";
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final SecretGenerator twoFactorSecretGenerator;
    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResourceLoader resourceLoader;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, FileStorageService fileStorageService, SecretGenerator twoFactorSecretGenerator, AppProperties appProperties, JwtTokenProvider jwtTokenProvider, TokenRepository tokenRepository, ResourceLoader resourceLoader) {
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.twoFactorSecretGenerator = twoFactorSecretGenerator;
        this.appProperties = appProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRepository = tokenRepository;
        this.resourceLoader = resourceLoader;
    }


    public JwtToken createToken(User user, String value, TokenType tokenType) {
        JwtToken jwtToken = new JwtToken();
        jwtToken.setValue(value);
        jwtToken.setUser(user);
        jwtToken.setTokenType(tokenType);
        return jwtToken;
    }

    public User createNewUser(SignUpRequest signUpRequest) throws IOException {
        User user = new User();
        user.setEmailVerified(false);
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());
        user.setProvider(AuthProvider.local);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setTwoFactorEnabled(false);
        user.setProfileImage(fileStorageService.store(resourceLoader.getResource("classpath:images\\blank-profile-picture.png").getInputStream(),"blank-profile-picture.png", "image/png"));
        return user;
    }

    public Cookie createRefreshTokenCookie(String refreshTokenValue, Integer refreshTokenExpirationMillis) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshTokenValue);
        cookie.setMaxAge(refreshTokenExpirationMillis);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    public Cookie createEmptyRefreshTokenCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(1);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    public User updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return user;
    }


    public Optional<JwtToken> getRefreshTokenFromRequest(HttpServletRequest request) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .flatMap(cookie -> tokenRepository.findByValueAndTokenType(cookie.getValue(), TokenType.REFRESH));
    }


}
