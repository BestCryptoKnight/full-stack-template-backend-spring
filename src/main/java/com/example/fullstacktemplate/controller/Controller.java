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
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
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
    @ExceptionHandler(EmailInUseException.class)
    public ApiResponse handleEmailInUseException() {
        return new ApiResponse(false, messageService.getMessage("emailInUse"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UsernameInUse.class)
    public ApiResponse handleUsernameInUseException() {
        return new ApiResponse(false, messageService.getMessage("usernameInUse"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UserNotFoundException.class)
    public ApiResponse handleUserNotFoundException() {
        return new ApiResponse(false, messageService.getMessage("userNotFound"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TokenExpiredException.class)
    public ApiResponse handleTokenExpiredException() {
        return new ApiResponse(false, messageService.getMessage("tokenExpired"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidTokenException.class)
    public ApiResponse handleInvalidTokenException() {
        return new ApiResponse(false, messageService.getMessage("invalidToken"));
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ApiResponse handleAnyException(HttpServletRequest request) {
        return new ApiResponse(false, messageService.getMessage("somethingWrong"));
    }

}
