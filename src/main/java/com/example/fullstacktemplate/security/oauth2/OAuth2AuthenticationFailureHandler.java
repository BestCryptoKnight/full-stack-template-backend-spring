package com.example.fullstacktemplate.security.oauth2;

import com.example.fullstacktemplate.util.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static com.example.fullstacktemplate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.LANGUAGE_COOKIE_NAME;
import static com.example.fullstacktemplate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Autowired
    protected ResourceBundleMessageSource messageSource;

    @Autowired
    protected  LocaleResolver acceptHeaderLocaleResolver;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(("/"));
        String language = CookieUtils.getCookie(request, LANGUAGE_COOKIE_NAME).map(Cookie::getValue).orElse("en");

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", URLEncoder.encode(messageSource.getMessage(exception.getLocalizedMessage(), null, new Locale(language))), StandardCharsets.UTF_8)
                .build().toUriString();

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
