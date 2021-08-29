package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.dto.ApiResponse;
import com.example.fullstacktemplate.exception.*;
import com.example.fullstacktemplate.repository.FileRepository;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.repository.TwoFactoryRecoveryCodeRepository;
import com.example.fullstacktemplate.security.CustomUserDetailsService;
import com.example.fullstacktemplate.security.JwtTokenProvider;
import com.example.fullstacktemplate.service.*;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

public abstract class Controller {


    @Autowired
    protected AuthenticationManager authenticationManager;

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
    protected AuthenticationService authenticationService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected FileRepository fileRepository;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        return new ApiResponse(false, ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map((error) -> messageService.getMessage(error.getDefaultMessage()))
                .collect(Collectors.joining(",")));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ApiResponse handleBadRequestException(BadRequestException ex) {
        return new ApiResponse(false, messageService.getMessage(ex.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {UnauthorizedRequestException.class, AuthenticationException.class})
    public ApiResponse handleUnauthorized() {
        return new ApiResponse(false, messageService.getMessage("invalidCredentials"));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ApiResponse handleAnyException() {
        return new ApiResponse(false, messageService.getMessage("somethingWrong"));
    }

}
