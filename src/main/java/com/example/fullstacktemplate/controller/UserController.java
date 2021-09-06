package com.example.fullstacktemplate.controller;

import com.example.fullstacktemplate.config.security.CurrentUser;
import com.example.fullstacktemplate.config.security.UserPrincipal;
import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.dto.mapper.UserMapper;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.User;
import dev.samstevens.totp.exceptions.QrGenerationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

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
        userService.updateProfile(userPrincipal.getId(), userDto);
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
        String accessToken = authenticationService.createAccessToken(user);
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
    public TwoFactorSetupDto getTwoFactorSetup(@CurrentUser UserPrincipal userPrincipal) throws QrGenerationException {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        return userService.getTwoFactorSetup(user);
    }

    @PostMapping("/verify-two-factor")
    public TwoFactorDto verifyTwoFactor(@CurrentUser UserPrincipal userPrincipal, @Valid @RequestBody TwoFactorVerificationRequestDto twoFactorVerificationRequestDto) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        return userService.verifyTwoFactor(user, twoFactorVerificationRequestDto.getCode());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId()).orElseThrow(() -> new BadRequestException("userNotFound"));
        authenticationService.logout(user);
        return ResponseEntity.ok(new ApiResponseDto(true, messageService.getMessage("loggedOut")));

    }
}
