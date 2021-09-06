package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TokenType;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.TokenRepository;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final AppProperties appProperties;
    private final TokenRepository tokenRepository;

    public TokenService(AppProperties appProperties, TokenRepository tokenRepository) {
        this.appProperties = appProperties;
        this.tokenRepository = tokenRepository;
    }

    public String createJwtTokenValue(Long id, Duration expireIn) {
        return createJwtTokenValue(Long.toString(id), expireIn);
    }

    private String createJwtTokenValue(String subject, Duration expireIn) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expireIn.toMillis());

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, appProperties.getAuth().getTokenSecret())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(appProperties.getAuth().getTokenSecret())
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateJwtToken(String jwtToken) {
        try {
            Jwts.parser().setSigningKey(appProperties.getAuth().getTokenSecret()).parseClaimsJws(jwtToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }

    @Transactional
    public JwtToken createToken(User user, Duration expireIn, TokenType tokenType) {
        String tokenValue = createJwtTokenValue(user.getId(), expireIn);
        JwtToken jwtToken = new JwtToken();
        jwtToken.setValue(tokenValue);
        jwtToken.setUser(user);
        jwtToken.setTokenType(tokenType);
        return tokenRepository.save(jwtToken);
    }

    public void delete(JwtToken jwtToken){
        tokenRepository.delete(jwtToken);
    }
}
