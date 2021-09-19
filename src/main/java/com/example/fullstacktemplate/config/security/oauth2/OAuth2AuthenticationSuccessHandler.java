package com.example.fullstacktemplate.config.security.oauth2;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.config.security.UserPrincipal;
import com.example.fullstacktemplate.dto.AuthResponseDto;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.service.*;
import com.example.fullstacktemplate.util.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService.*;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final AppProperties appProperties;
    private final CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService;
    private final TokenRepository tokenRepository;
    private final UserService userService;
    private final MessageService messageService;
    private final AuthenticationService authenticationService;

    @Autowired
    OAuth2AuthenticationSuccessHandler(TokenService tokenService, AppProperties appProperties,
                                       CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService,
                                       UserService userService, TokenRepository tokenRepository, MessageService messageService, AuthenticationService authenticationService) {
        this.tokenService = tokenService;
        this.appProperties = appProperties;
        this.cookieOAuth2AuthorizationRequestService = cookieOAuth2AuthorizationRequestService;
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.messageService = messageService;
        this.authenticationService = authenticationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            String targetUrl = determineTargetUrl(request, response, authentication);
            if (response.isCommitted()) {
                logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
                return;
            }
            clearAuthenticationAttributes(request, response);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } finally {
            clearAuthenticationAttributes(request, response);
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException(messageService.getMessage("o2authInvalidTargetUrl"));
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        User user = userService.findById((((UserPrincipal) authentication.getPrincipal()).getId())).orElseThrow(() -> new BadRequestException("userNotFound"));
        if (user.getTwoFactorEnabled()) {
            return determineTargetTwoFactorUrl(request, redirectUri, authentication);
        }
        AuthResponseDto authResponseDto = authenticationService.login((UserPrincipal) authentication.getPrincipal());
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("access_token", authResponseDto.getAccessToken())
                .build().toUriString();

    }

    private String determineTargetTwoFactorUrl(HttpServletRequest request, Optional<String> redirectUri, Authentication authentication) {
        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());
        try {
            Optional<String> twoFactorCode = CookieUtils.getCookie(request, TWO_FACTOR_CODE).map(Cookie::getValue);
            Optional<String> recoveryCode = CookieUtils.getCookie(request, RECOVERY_CODE).map(Cookie::getValue);
            if (twoFactorCode.isEmpty() && recoveryCode.isEmpty()) {
                return UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("two_factor_required", true)
                        .build().toUriString();
            }
            AuthResponseDto authResponseDto;
            if (twoFactorCode.isPresent()) {
                authResponseDto = authenticationService.loginWithVerificationCode((UserPrincipal) authentication.getPrincipal(), twoFactorCode.get());
            } else {
                authResponseDto = authenticationService.loginWithRecoveryCode((UserPrincipal) authentication.getPrincipal(), recoveryCode.get());
            }
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("access_token", authResponseDto.getAccessToken())
                    .build().toUriString();
        } catch (BadRequestException e) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("two_factor_required", true)
                    .queryParam("error", messageService.getMessage(e.getLocalizedMessage()))
                    .build().toUriString();
        }
    }


    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieOAuth2AuthorizationRequestService.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);
        return appProperties.getAuthorizedRedirectUris()
                .stream()
                .map(URI::create)
                .anyMatch(authorizedURI ->
                        authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost()) &&
                                authorizedURI.getPort() == clientRedirectUri.getPort()
                );
    }
}
