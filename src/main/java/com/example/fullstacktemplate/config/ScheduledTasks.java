package com.example.fullstacktemplate.config;

import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.service.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScheduledTasks {
    private final TokenRepository tokenRepository;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public ScheduledTasks(TokenRepository tokenRepository, JwtTokenService jwtTokenService) {
        this.tokenRepository = tokenRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Scheduled(fixedDelayString = "${app.deleteExpiredTokensDelayMsec}")
    public void deleteExpiredTokens() {
        log.info("Deleting expired tokens");
        List<JwtToken> expiredTokens = tokenRepository
                .findAll()
                .stream()
                .filter(jwtToken -> !jwtTokenService.validateToken(jwtToken.getValue()))
                .collect(Collectors.toList());
        tokenRepository.deleteAll(expiredTokens);
        log.info("Following tokens were deleted {}", expiredTokens);
    }
}
