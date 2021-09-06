package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController extends Controller {


    @PostMapping("/login")
    public AuthResponseDto authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return authenticationService.login(loginRequestDto);
    }

    @PostMapping("/login/verify")
    public AuthResponseDto verifyLogin(@Valid @RequestBody LoginVerificationRequestDto loginVerificationRequestDto) {
        return authenticationService.loginWithVerificationCode(loginVerificationRequestDto);
    }

    @PostMapping("/login/recovery-code")
    public AuthResponseDto loginRecoveryCode(@Valid @RequestBody LoginVerificationRequestDto loginVerificationRequestDto) {
        return authenticationService.loginWithRecoveryCode(loginVerificationRequestDto);
    }

    @GetMapping("/access-token")
    public TokenResponseDto refreshAuth() {
        Optional<JwtToken> optionalRefreshToken = authenticationService.getRefreshToken();
        if (optionalRefreshToken.isPresent()) {
            Optional<User> optionalUser = userService.findById(tokenService.getUserIdFromToken(optionalRefreshToken.get().getValue()));
            if (optionalUser.isPresent() && optionalRefreshToken.get().getUser().getId().equals(optionalUser.get().getId())) {
                return new TokenResponseDto(authenticationService.createAccessToken(optionalUser.get()));
            }
        }
        throw new BadRequestException("tokenExpired");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) throws URISyntaxException, IOException {
        userService.createNewUser(signUpRequestDto);
        return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("userWasRegistered")));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<?> activateUserAccount(@Valid @RequestBody TokenAccessRequestDto tokenAccessRequestDto) {
        userService.activateUserAccount(tokenAccessRequestDto);
        return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("accountActivated")));
    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<?> confirmEmailChange(@Valid @RequestBody TokenAccessRequestDto tokenAccessRequestDto) {
        userService.activateRequestedEmail(tokenAccessRequestDto);
        return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("emailUpdated")));
    }

    @PostMapping("/forgotten-password")
    public ResponseEntity<?> forgottenPassword(@Valid @RequestBody ForgottenPasswordRequestDto forgottenPasswordRequestDto) throws MalformedURLException, URISyntaxException {
        User user = userService.findByEmail(forgottenPasswordRequestDto.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getEmailVerified()) {
            userService.requestPasswordReset(user);
            return ResponseEntity
                    .ok(new ApiResponseDto(true, messageService.getMessage("passwordResetEmailSentMessage")));
        } else {
            throw new BadRequestException("accountNotActivated");
        }
    }

    @PostMapping("/password-reset")
    public ResponseEntity<?> passwordReset(@Valid @RequestBody PasswordResetRequestDto passwordResetRequestDto) {
        User user = userService.findByEmail(passwordResetRequestDto.getEmail()).orElseThrow(() -> new BadRequestException("userNotFound"));
        userService.resetPassword(user, passwordResetRequestDto.getToken());
        return ResponseEntity
                .ok(new ApiResponseDto(true, messageService.getMessage("passwordWasReset")));

    }


}
