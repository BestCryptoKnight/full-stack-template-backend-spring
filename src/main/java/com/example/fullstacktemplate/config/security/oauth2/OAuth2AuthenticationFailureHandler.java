package com.example.fullstacktemplate.config.security.oauth2;

import com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService;
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

import static com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService.LANGUAGE_COOKIE_NAME;
import static com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private  final CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService;

    public OAuth2AuthenticationFailureHandler(CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService, ResourceBundleMessageSource messageSource, LocaleResolver acceptHeaderLocaleResolver) {
        this.cookieOAuth2AuthorizationRequestService = cookieOAuth2AuthorizationRequestService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(("/"));

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", URLEncoder.encode(exception.getLocalizedMessage()), StandardCharsets.UTF_8)
                .build().toUriString();

        cookieOAuth2AuthorizationRequestService.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
