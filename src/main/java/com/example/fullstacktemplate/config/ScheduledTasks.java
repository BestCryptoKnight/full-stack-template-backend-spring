package com.example.fullstacktemplate.config;

import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScheduledTasks {
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    public ScheduledTasks(TokenRepository tokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Scheduled(fixedDelayString = "${app.deleteExpiredTokensDelayMsec}")
    public void deleteExpiredTokens() {
        log.info("Deleting expired tokens");
        List<JwtToken> expiredTokens = tokenRepository
                .findAll()
                .stream()
                .filter(jwtToken -> !jwtTokenProvider.validateToken(jwtToken.getValue()))
                .collect(Collectors.toList());
        tokenRepository.deleteAll(expiredTokens);
        log.info("Following tokens were deleted {}", expiredTokens);
    }
}
