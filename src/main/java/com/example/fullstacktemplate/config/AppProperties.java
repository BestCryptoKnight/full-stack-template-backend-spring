package com.example.fullstacktemplate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private final Auth auth = new Auth();
    private List<String> authorizedRedirectUris;
    private List<String> allowedOrigins;
    private String accountActivationUri;
    private String emailChangeConfirmationUri;
    private String passwordResetUri;
    private String appName;

    public static class Auth {
        private String tokenSecret;
        private long accessTokenExpirationMsec;
        private long persistentTokenExpirationMsec;
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

        public long getPersistentTokenExpirationMsec() {
            return persistentTokenExpirationMsec;
        }

        public void setPersistentTokenExpirationMsec(long persistentTokenExpirationMsec) {
            this.persistentTokenExpirationMsec = persistentTokenExpirationMsec;
        }

        public long getVerificationTokenExpirationMsec() {
            return verificationTokenExpirationMsec;
        }

        public void setVerificationTokenExpirationMsec(long verificationTokenExpirationMsec) {
            this.verificationTokenExpirationMsec = verificationTokenExpirationMsec;
        }
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        acceptHeaderLocaleResolver.setSupportedLocales(Arrays.asList(Locale.ENGLISH, new Locale("cs")));
        return acceptHeaderLocaleResolver;
    }
}
