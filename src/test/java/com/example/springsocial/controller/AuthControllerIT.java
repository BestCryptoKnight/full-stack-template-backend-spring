package com.example.springsocial.controller;

import com.example.springsocial.config.AppProperties;
import com.example.springsocial.model.*;
import com.example.springsocial.payload.*;
import com.example.springsocial.repository.TokenRepository;
import com.example.springsocial.repository.UserRepository;
import com.example.springsocial.security.JwtTokenProvider;
import com.google.gson.Gson;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.javers.core.diff.ListCompareAlgorithm.LEVENSHTEIN_DISTANCE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Tag("Integration")
public class AuthControllerIT {


    private static final String USERNAME = "test";
    private static final String EMAIL = "test1@email.com";
    private static final String VALID_PASSWORD = "Valid_password1";

    private static final String EMPTY_STRING = "";
    private static final String VALID_VERIFICATION_TOKEN = "VALID_TOKEN";
    private static final String INVALID_VERIFICATION_TOKEN = "INVALID_TOKEN";
    private static final Gson GSON = new Gson();
    private static final Javers JAVERS = JaversBuilder
            .javers()
            .withListCompareAlgorithm(LEVENSHTEIN_DISTANCE)
            .build();
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AppProperties appProperties;

    private GreenMail smtpServer;

    @Value(value = "${spring.mail.port}")
    private Integer smtpPort;

    @BeforeAll
    public static void beforeAll() {

    }


    @BeforeEach
    public void beforeEach() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void loginTest_validEmailAndPassword_ShouldLogIn() throws Exception {
        User user = userRepository.save(createUser(USERNAME, EMAIL, passwordEncoder.encode(VALID_PASSWORD), true, false));

        MvcResult mvcResult = loginWithCredentials(EMAIL, VALID_PASSWORD);
        AuthResponse authResponse = GSON.fromJson(mvcResult.getResponse().getContentAsString(), AuthResponse.class);

        assertAll(
                () -> assertEquals(200, mvcResult.getResponse().getStatus()),
                () -> assertEquals(user.getId(), jwtTokenProvider.getUserIdFromToken(authResponse.getAccessToken()))

        );
    }

    @Test
    public void loginTest_validEmailAndPasswordAndRememberMeTrue_ShouldLogIn() throws Exception {
        User user = userRepository.save(createUser(USERNAME, EMAIL, passwordEncoder.encode(VALID_PASSWORD), true, false));

        MvcResult mvcResult = loginWithCredentials(EMAIL, VALID_PASSWORD, true);
        AuthResponse authResponse = GSON.fromJson(mvcResult.getResponse().getContentAsString(), AuthResponse.class);

        assertAll(
                () -> assertEquals(200, mvcResult.getResponse().getStatus()),
                () -> assertEquals(user.getId(), jwtTokenProvider.getUserIdFromToken(authResponse.getAccessToken()))

        );
    }

    @Test
    public void loginTest_invalidCredentials_ShouldNotLogIn() throws Exception {
        MvcResult mvcResult = loginWithCredentials(EMAIL, VALID_PASSWORD);

        assertEndpointResult(mvcResult, 401, new ApiResponse(false, "Bad credentials"));
    }

    @Test
    public void activateUserAccountTest_existingUserAndValidNotExpiredToken_ShouldActivate() throws Exception {
        User userWithoutActivatedAccount = userRepository.save(createUser(USERNAME, EMAIL, passwordEncoder.encode(VALID_PASSWORD), false, false));
        String token = jwtTokenProvider.createToken(userWithoutActivatedAccount, Duration.of(appProperties.getAuth().getVerificationTokenExpirationMsec(), ChronoUnit.MILLIS));
        tokenRepository.save(createToken(userWithoutActivatedAccount, token, TokenType.ACCOUNT_ACTIVATION));

        MvcResult mvcResult = activateAccount(EMAIL, token);

        assertAll(
                () -> assertEndpointResult(mvcResult, 200, new ApiResponse(true, "Account has been activated")),
                () -> assertTrue(userRepository.findByEmail(EMAIL).get().getEmailVerified(), "User account has not been activated"),
                () -> assertFalse(tokenRepository.findByUserAndTokenType(userWithoutActivatedAccount, TokenType.ACCOUNT_ACTIVATION).isPresent(), "Verification token has not been deleted")

        );

    }

    @Test
    public void activateUserAccountTest_existingUserAndValidExpiredToken_ShouldNotActivate() throws Exception {
        User existingUser = userRepository.save(createUser(USERNAME, EMAIL, passwordEncoder.encode(VALID_PASSWORD), false, false));
        String token = jwtTokenProvider.createToken(existingUser, Duration.of(0L, ChronoUnit.MILLIS));
        tokenRepository.save(createToken(existingUser, token, TokenType.ACCOUNT_ACTIVATION));

        MvcResult mvcResult = activateAccount(EMAIL, token);

        assertEndpointResult(mvcResult, 400, new ApiResponse(false, "Token is expired"));
    }

    @Test
    public void activateUserAccountTest_existingUserAndInValidNotExpiredTokenToken_ShouldNotActivate() throws Exception {
        User existingUser = userRepository.save(createUser(USERNAME, EMAIL, passwordEncoder.encode(VALID_PASSWORD), false, false));
        tokenRepository.save(createToken(existingUser, VALID_VERIFICATION_TOKEN, TokenType.ACCOUNT_ACTIVATION));


        MvcResult mvcResult = activateAccount(EMAIL, INVALID_VERIFICATION_TOKEN);

        assertEndpointResult(mvcResult, 400, new ApiResponse(false, "Invalid jwtToken"));
    }

    @Test
    public void signUpTest_signUpUserAllParametersValid_ShouldCreateUserAndSendActivationEmail() throws Exception {
        smtpServer = new GreenMail(new ServerSetup(this.smtpPort, null, "smtp"));
        smtpServer.start();

        MvcResult mvcResult = register(USERNAME, EMAIL, VALID_PASSWORD);

        MimeMessage[] receivedMessages = smtpServer.getReceivedMessages();
        Optional<MimeMessage> receivedMessage = Arrays.stream(receivedMessages).findFirst();
        Optional<User> optionalUser = userRepository.findByEmail(EMAIL);
        assertAll(
                () -> assertEndpointResult(mvcResult, 201, new ApiResponse(true, "User registered successfully. Activate your account via email")),
                () -> assertEquals(1, receivedMessages.length),
                () -> assertTrue(optionalUser.isPresent()),
                () -> optionalUser.ifPresent(user -> {
                    assertAll(
                            () -> assertEquals(USERNAME, user.getName()),
                            () -> assertEquals(EMAIL, user.getEmail()),
                            () -> assertTrue(passwordEncoder.matches(VALID_PASSWORD, user.getPassword())),
                            () -> assertFalse(user.getEmailVerified())
                    );
                }),
                () -> {
                    receivedMessage.ifPresent(message ->
                            assertAll(
                                    () -> {
                                        String verificationTokenValue = Optional.ofNullable(tokenRepository.findByUserAndTokenType(optionalUser.get(), TokenType.ACCOUNT_ACTIVATION)).map(jwtToken -> jwtToken.get().getValue()).orElse("");
                                        try {
                                            assertEquals(String.format("Activate your account using following link http://test.com/activate-account?token=%s", verificationTokenValue), message.getContent().toString().trim());
                                        } catch (IOException | MessagingException e) {
                                            e.printStackTrace();
                                        }
                                    },
                                    () -> assertEquals("FullStack template account activation", message.getSubject())));
                });
        smtpServer.stop();
    }

    private static LoginRequest createSignInRequest(String email, String password, Boolean rememberMe) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        loginRequest.setRememberMe(rememberMe);
        return loginRequest;
    }

    private static User createUser(String username, String email, String password, Boolean emailVerified, Boolean twoFactorEnabled) {
        User user = new User();
        user.setProvider(AuthProvider.local);
        user.setName(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setTwoFactorEnabled(twoFactorEnabled);
        user.setEmailVerified(emailVerified);
        return user;
    }

    private MvcResult loginWithCredentials(String email, String password, Boolean rememberMe) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(GSON.toJson(createSignInRequest(email, password, rememberMe))))
                .andReturn();
    }

    private MvcResult loginWithCredentials(String email, String password) throws Exception {
        return loginWithCredentials(email, password, false);
    }

    private MvcResult activateAccount(String email, String token) throws Exception {
        TokenAccessRequest tokenAccessRequest = new TokenAccessRequest();
        tokenAccessRequest.setToken(token);
        return mvc.perform(post("/auth/activateAccount")
                .contentType("application/json")
                .content(GSON.toJson(tokenAccessRequest)))
                .andReturn();
    }

    private MvcResult register(String name, String email, String password) throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setName(name);
        signUpRequest.setEmail(email);
        signUpRequest.setPassword(password);
        return mvc.perform(post("/auth/signup")
                .contentType("application/json")
                .content(GSON.toJson(signUpRequest)))
                .andReturn();
    }


    private JwtToken createToken(User user, String tokeValue, TokenType tokenType) {
        JwtToken token = new JwtToken();
        token.setId(1L);
        token.setUser(user);
        token.setValue(tokeValue);
        token.setTokenType(tokenType);
        return token;
    }

    private static void assertEndpointResult(MvcResult mvcResult, Integer expectedStatus, Object expectedResponse) throws UnsupportedEncodingException {
        Diff diff = JAVERS.compare(expectedResponse, GSON.fromJson(mvcResult.getResponse().getContentAsString(), expectedResponse.getClass()));
        assertAll(
                () -> assertEquals(expectedStatus, mvcResult.getResponse().getStatus()),
                () -> assertFalse(diff.hasChanges(), "Returned object is not same as expected " + diff.prettyPrint())


        );

    }
}
