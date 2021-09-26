package com.example.fullstacktemplate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@EnableAsync
public class AppProperties {
    private final Auth auth = new Auth();
    private List<String> authorizedRedirectUris;
    private List<String> allowedOrigins;
    private String accountActivationUri;
    private String emailChangeConfirmationUri;
    private String passwordResetUri;
    private String appName;
    private int maxRequestSize;

    public static class Auth {
        private String tokenSecret;
        private long accessTokenExpirationMsec;
        private long refreshTokenExpirationMsec;
        private long verificationTokenExpirationMsec;

        public String getTokenSecret() {
            return tokenSecret;
        }

        public void setTokenSecret(String tokenSecret) {
            this.tokenSecret = tokenSecret;
        }

        public long getAccessTokenExpirationMsec() {
            return accessTokenExpirationMsec;
        }

        public void setAccessTokenExpirationMsec(long accessTokenExpirationMsec) {
            this.accessTokenExpirationMsec = accessTokenExpirationMsec;
        }

        public long getRefreshTokenExpirationMsec() {
            return refreshTokenExpirationMsec;
        }

        public void setRefreshTokenExpirationMsec(long refreshTokenExpirationMsec) {
            this.refreshTokenExpirationMsec = refreshTokenExpirationMsec;
        }

        public long getVerificationTokenExpirationMsec() {
            return verificationTokenExpirationMsec;
        }

        public void setVerificationTokenExpirationMsec(long verificationTokenExpirationMsec) {
            this.verificationTokenExpirationMsec = verificationTokenExpirationMsec;
        }
    }

}
