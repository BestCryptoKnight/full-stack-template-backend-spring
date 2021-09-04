package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.dto.ApiResponseDto;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.exception.UnauthorizedRequestException;
import com.example.fullstacktemplate.repository.FileDbRepository;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.repository.TwoFactoryRecoveryCodeRepository;
import com.example.fullstacktemplate.service.CustomUserDetailsService;
import com.example.fullstacktemplate.service.JwtTokenService;
import com.example.fullstacktemplate.service.*;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@Slf4j
public abstract class Controller {


    @Autowired
    protected AuthenticationManager authenticationManager;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtTokenService jwtTokenService;
    @Autowired
    protected EmailService emailService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected TokenRepository tokenRepository;
    @Autowired
    protected FileDbService storageService;
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
    protected FileDbRepository fileDbRepository;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponseDto handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        return new ApiResponseDto(false, ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map((error) -> messageService.getMessage(error.getDefaultMessage()))
                .collect(Collectors.joining(",")));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ApiResponseDto handleBadRequestException(BadRequestException ex) {
        return new ApiResponseDto(false, messageService.getMessage(ex.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {UnauthorizedRequestException.class, AuthenticationException.class})
    public ApiResponseDto handleUnauthorized() {
        return new ApiResponseDto(false, messageService.getMessage("invalidCredentials"));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ApiResponseDto handleAnyException(Exception e) {
        log.error("Error while processing exception",e);
        return new ApiResponseDto(false, messageService.getMessage("somethingWrong"));
    }

}
