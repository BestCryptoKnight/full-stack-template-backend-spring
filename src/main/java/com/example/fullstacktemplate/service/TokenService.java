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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.transaction.Transactional;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final AppProperties appProperties;
    private final TokenRepository tokenRepository;
    private final CryptoService cryptoService;
    private final SecretKey secretKey;
    private final String algorithm;
    private final IvParameterSpec ivParameterSpec;

    public TokenService(AppProperties appProperties, TokenRepository tokenRepository, CryptoService cryptoService) throws NoSuchAlgorithmException {
        this.appProperties = appProperties;
        this.tokenRepository = tokenRepository;
        this.cryptoService = cryptoService;
        this.algorithm = "AES/CBC/PKCS5Padding";
        this.ivParameterSpec = cryptoService.generateInitializationVector();
        this.secretKey = cryptoService.generateKey(256);
    }

    public String createJwtTokenValue(Long id, Duration expireIn) {
        return createJwtTokenValue(Long.toString(id), expireIn);
    }

    private String createJwtTokenValue(String subject, Duration expireIn) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expireIn.toMillis());
        try {
            return Jwts.builder()
                    .setSubject(cryptoService.encrypt(algorithm, subject, secretKey, ivParameterSpec))
                    .setIssuedAt(new Date())
                    .setExpiration(expiryDate)
                    .setIssuer("Full stack template")
                    .signWith(SignatureAlgorithm.HS512, appProperties.getAuth().getTokenSecret())
                    .compact();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException("Error while creating jwt token");
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(appProperties.getAuth().getTokenSecret())
                .parseClaimsJws(token)
                .getBody();
        try {
            return Long.parseLong(cryptoService.decrypt(algorithm, claims.getSubject(), secretKey, ivParameterSpec));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException("Error while getting id from token");
        }
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

    public void delete(JwtToken jwtToken) {
        tokenRepository.delete(jwtToken);
    }
}
