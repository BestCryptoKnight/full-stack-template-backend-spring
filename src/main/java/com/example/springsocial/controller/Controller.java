package com.example.springsocial.controller;

import com.example.springsocial.config.AppProperties;
import com.example.springsocial.payload.ApiResponse;
import com.example.springsocial.repository.*;
import com.example.springsocial.security.CustomUserDetailsService;
import com.example.springsocial.security.JwtTokenProvider;
import com.example.springsocial.service.EmailService;
import com.example.springsocial.service.FileStorageService;
import com.example.springsocial.service.UserService;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

public abstract class Controller {


    @Autowired
    protected AuthenticationManager authenticationManager;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected EmailService emailService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected TokenRepository tokenRepository;

    @Autowired
    protected FileStorageService storageService;

    @Autowired
    protected AppProperties appProperties;

    @Autowired
    protected SecretGenerator twoFactorSecretGenerator;

    @Autowired
    protected CustomUserDetailsService customUserDetailsService;

    @Autowired
    protected TwoFactoryRecoveryCodeRepository twoFactoryRecoveryCodeRepository;


    @Autowired
    protected ResourceBundleMessageSource messageSource;

    @Autowired
    protected LocaleResolver localeResolver;
    @Autowired
    protected FileRepository fileRepository;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return new ApiResponse(false, ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map((error) -> messageSource.getMessage(error.getDefaultMessage(), null, localeResolver.resolveLocale(request)))
                .collect(Collectors.joining(",")));
    }

}
