package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.dto.*;
import com.example.fullstacktemplate.dto.mapper.UserMapper;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.exception.UnauthorizedRequestException;
import com.example.fullstacktemplate.model.*;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.repository.TwoFactoryRecoveryCodeRepository;
import com.example.fullstacktemplate.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final FileDbService fileDbService;
    private final SecretGenerator twoFactorSecretGenerator;
    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;
    private final TokenService tokenService;
    private final ResourceLoader resourceLoader;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final TwoFactoryRecoveryCodeRepository twoFactoryRecoveryCodeRepository;
    private final MessageService messageService;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, FileDbService fileDbService, SecretGenerator twoFactorSecretGenerator, AppProperties appProperties, TokenService tokenService, TokenRepository tokenRepository, ResourceLoader resourceLoader, UserRepository userRepository, EmailService emailService, UserMapper userMapper, TwoFactoryRecoveryCodeRepository twoFactoryRecoveryCodeRepository, MessageService messageService) {
        this.passwordEncoder = passwordEncoder;
        this.fileDbService = fileDbService;
        this.twoFactorSecretGenerator = twoFactorSecretGenerator;
        this.appProperties = appProperties;
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        this.resourceLoader = resourceLoader;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.twoFactoryRecoveryCodeRepository = twoFactoryRecoveryCodeRepository;
        this.messageService = messageService;
    }

    public User createNewUser(SignUpRequestDto signUpRequestDto) throws IOException, URISyntaxException {
        if (isEmailUsed(signUpRequestDto.getEmail())) {
            log.error("Email {} is already used",signUpRequestDto.getEmail());
            throw new BadRequestException("emailInUse");
        }
        if (isUsernameUsed(signUpRequestDto.getName())) {
            log.error("Username {} is already used",signUpRequestDto.getName());
            throw new BadRequestException("usernameInUse");
        }
        User user = new User();
        user.setEmailVerified(false);
        user.setName(signUpRequestDto.getName());
        user.setEmail(signUpRequestDto.getEmail());
        user.setAuthProvider(AuthProvider.local);
        user.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        user.setTwoFactorEnabled(false);
        user.setRole(Role.USER);
        user.setProfileImage(fileDbService.save("blank-profile-picture.png", FileType.IMAGE_PNG, resourceLoader.getResource("classpath:images\\blank-profile-picture.png").getInputStream().readAllBytes()));
        user = userRepository.save(user);
        JwtToken jwtToken = tokenService.createToken(user, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS), TokenType.ACCOUNT_ACTIVATION);
        URIBuilder uriBuilder = new URIBuilder(appProperties.getAccountActivationUri())
                .addParameter("token", jwtToken.getValue());
        emailService.sendSimpleMessage(
                signUpRequestDto.getEmail(),
                appProperties.getAppName() + " " + messageService.getMessage("activateAccountEmailSubject"),
                String.format("%s %s", messageService.getMessage("activateAccountEmailBody"), uriBuilder.build().toURL().toString()));
        return user;
    }

    public User updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void cancelUserAccount(Long userId) {
        userRepository.deleteById(userId);
    }

    public User updatePassword(User user, ChangePasswordDto changePasswordDto) {
        if (passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            return userRepository.save(user);
        } else {
            throw new UnauthorizedRequestException();
        }
    }

    public User activateUserAccount(TokenAccessRequestDto tokenAccessRequestDto) {
        Optional<JwtToken> optionalVerificationToken = tokenRepository.findByValueAndTokenType(tokenAccessRequestDto.getToken(), TokenType.ACCOUNT_ACTIVATION);
        if (optionalVerificationToken.isPresent()) {
            User user = optionalVerificationToken.get().getUser();
            if (!tokenService.validateJwtToken(tokenAccessRequestDto.getToken())) {
                throw new BadRequestException("tokenExpired");
            } else {
                user.setEmailVerified(true);
                userRepository.save(user);
                tokenRepository.delete(optionalVerificationToken.get());
            }
            return userRepository.save(user);
        }
        throw new BadRequestException("invalidToken");
    }

    public User disableTwoFactorAuthentication(User user) {
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        user.getTwoFactorRecoveryCodes().clear();
        return userRepository.save(user);
    }

    public User enableTwoFactorAuthentication(User user) {
        user.setTwoFactorEnabled(true);
        return userRepository.save(user);
    }

    public User activateRequestedEmail(TokenAccessRequestDto tokenAccessRequestDto) {
        Optional<JwtToken> optionalVerificationToken = tokenRepository.findByValueAndTokenType(tokenAccessRequestDto.getToken(), TokenType.EMAIL_UPDATE);
        if (optionalVerificationToken.isPresent()) {
            User user = optionalVerificationToken.get().getUser();
            if (!tokenService.validateJwtToken(tokenAccessRequestDto.getToken())) {
                throw new BadRequestException("tokenExpired");
            } else {
                user.setEmail(user.getRequestedNewEmail());
                user.setRequestedNewEmail(null);
                userRepository.save(user);
                tokenRepository.delete(optionalVerificationToken.get());
                return user;
            }
        }
        throw new BadRequestException("invalidToken");
    }

    public User updateProfile(Long currentUserId, UserDto newUser) throws MalformedURLException, URISyntaxException {
        User user = findById(currentUserId).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (!newUser.getEmail().equals(user.getEmail()) && isEmailUsed(newUser.getEmail())) {
            throw new BadRequestException("emailInUse");
        }
        if (!newUser.getName().equals(user.getName()) && isUsernameUsed(newUser.getName())) {
            throw new BadRequestException("usernameInUse");
        }
        String newEmail = newUser.getEmail();
        String oldEmail = user.getEmail();
        if (user.getEmail() != null && !user.getEmail().equals(newUser.getEmail())) {
            JwtToken jwtToken = tokenService.createToken(user,  Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS), TokenType.EMAIL_UPDATE);
            URIBuilder uriBuilder = new URIBuilder(appProperties.getEmailChangeConfirmationUri())
                    .addParameter("token", jwtToken.getValue());
            emailService.sendSimpleMessage(
                    newEmail,
                    messageService.getMessage("confirmAccountEmailChangeEmailSubject", new Object[]{appProperties.getAppName()}),
                    messageService.getMessage("confirmAccountEmailChangeEmailBody", new Object[]{oldEmail, newEmail, uriBuilder.build().toURL().toString()})
            );
        }
        return userRepository.save(userMapper.toEntity(currentUserId,newUser));
    }

    public User setNewTwoFactorSecret(User user){
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        userRepository.save(user);
        return userRepository.save(user);
    }

    public void requestPasswordReset(User user) throws MalformedURLException, URISyntaxException {
        Optional<JwtToken> forgottenPasswordToken = tokenRepository.findByUserAndTokenType(user, TokenType.FORGOTTEN_PASSWORD);
        if (forgottenPasswordToken.isPresent()){
            log.info("There already is token of type {} for user {}. Going to delete it and issue a new one",TokenType.FORGOTTEN_PASSWORD,user.getName());
            tokenService.delete(forgottenPasswordToken.get());
        }
        JwtToken jwtToken = tokenService.createToken(user, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS), TokenType.FORGOTTEN_PASSWORD);
        URIBuilder uriBuilder = new URIBuilder(appProperties.getPasswordResetUri())
                .addParameter("email", user.getEmail())
                .addParameter("token", jwtToken.getValue());
        emailService.sendSimpleMessage(
                user.getEmail(),
                appProperties.getAppName() + " " + messageService.getMessage("passwordResetEmailSubject"),
                String.format("%s %s", messageService.getMessage("passwordResetEmailBody"), uriBuilder.build().toURL().toString())
        );
    }

    public void resetPassword(User user, PasswordResetRequestDto passwordResetRequestDto) {
        Optional<JwtToken> forgottenPasswordToken = tokenRepository.findByUserAndTokenType(user, TokenType.FORGOTTEN_PASSWORD);
        if (forgottenPasswordToken.isEmpty() || !forgottenPasswordToken.get().getValue().equals(passwordResetRequestDto.getToken())) {
            throw new BadRequestException("invalidToken");
        } else if (!tokenService.validateJwtToken(passwordResetRequestDto.getToken())) {
            throw new BadRequestException("tokenExpired");
        } else {
            updateUserPassword(user, passwordResetRequestDto.getPassword());
            tokenRepository.delete(forgottenPasswordToken.get());
        }
    }

    public TwoFactorSetupDto getTwoFactorSetup(User user) throws QrGenerationException {
        user = setNewTwoFactorSecret(user);
        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(user.getTwoFactorSecret())
                .issuer(appProperties.getAppName())
                .algorithm(HashingAlgorithm.SHA512)
                .digits(6)
                .period(30)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        TwoFactorSetupDto twoFactorSetupDto = new TwoFactorSetupDto();
        twoFactorSetupDto.setQrData(generator.generate(data));
        twoFactorSetupDto.setMimeType(generator.getImageMimeType());
        return twoFactorSetupDto;
    }

    public TwoFactorDto verifyTwoFactor(User user, String code){
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
        if (verifier.isValidCode(user.getTwoFactorSecret(), code)) {
            user = enableTwoFactorAuthentication(user);
            TwoFactorDto twoFactorDto = new TwoFactorDto();
            User finalUser = user;
            List<TwoFactorRecoveryCode> twoFactorRecoveryCodes = Arrays.asList(recoveryCodeGenerator.generateCodes(16))
                    .stream()
                    .map(recoveryCode -> {
                        TwoFactorRecoveryCode twoFactorRecoveryCode = new TwoFactorRecoveryCode();
                        twoFactorRecoveryCode.setRecoveryCode(recoveryCode);
                        twoFactorRecoveryCode.setUser(finalUser);
                        return twoFactorRecoveryCode;
                    })
                    .collect(Collectors.toList());
            twoFactorRecoveryCodes = twoFactoryRecoveryCodeRepository.saveAll(twoFactorRecoveryCodes);
            twoFactorDto.setVerificationCodes(twoFactorRecoveryCodes.stream().map(TwoFactorRecoveryCode::getRecoveryCode).collect(Collectors.toList()));
            return twoFactorDto;
        }
        throw new BadRequestException("invalidVerificationCode");
    }


    public boolean isUsernameUsed(String username) {
        return userRepository.existsByName(username);
    }

    public boolean isEmailUsed(String email) {
        return userRepository.existsByEmail(email);
    }
}
