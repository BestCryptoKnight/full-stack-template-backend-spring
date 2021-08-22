package com.example.fullstacktemplate;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.model.AuthProvider;
import com.example.fullstacktemplate.model.FileDb;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.FileRepository;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.repository.UserRepository;
import com.example.fullstacktemplate.security.JwtTokenProvider;
import com.example.fullstacktemplate.service.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class SpringSocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringSocialApplication.class, args);
    }


    /**
     * Fills the application with data
     *
     * @param fileRepository
     * @return
     */
    @Bean
    public ApplicationRunner initializer
    (
            FileRepository fileRepository,
            UserService userService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AppProperties appProperties,
            TokenRepository tokenRepository,
            ResourceLoader resourceLoader


    ) {
        return args -> {
            List<JwtToken> tokens = new LinkedList<>();
            List<FileDb> files = new LinkedList<>();
            List<User> users = new LinkedList<>();
            Random random = new Random();
            for (Integer i = 1; i <= 10; i++) {
                String suffix = i == 1 ? "" : i.toString();
               InputStream inputStream = resourceLoader.getResource("classpath:images\\blank-profile-picture.png").getInputStream();
                FileDb fileDb = new FileDb("blank-profile-picture.png", "image/png", inputStream.readAllBytes());
                fileDb.setId((long) i);
                files.add(fileDb);
                User user = new User();
                user.setId((long) i);
                user.setEmailVerified(false);
                user.setName("Test" + suffix);
                user.setEmail("hradil.o@email.cz" + suffix);
                user.setProvider(AuthProvider.local);
                user.setPassword(passwordEncoder.encode("test" + suffix));
                user.setTwoFactorEnabled(false);
                user.setTwoFactorEnabled(false);
                user.setEmailVerified(true);
                user.setProfileImage(fileDb);
                users.add(user);
/*

                for (Integer j = 1; j <= 1000; j++) {
                    String tokenValue = jwtTokenProvider.createToken(user, Duration.of(0L, ChronoUnit.MILLIS));
                    JwtToken jwtToken = userService.createToken(user, tokenValue, TokenType.values()[random.nextInt(TokenType.values().length)]);
                    jwtToken.setId((long) i * j);
                    jwtToken.setUser(user);
                    tokens.add(jwtToken);
                }*/

            }
            fileRepository.saveAll(files);
            userRepository.saveAll(users);
            tokenRepository.saveAll(tokens);

        };
    }
}
