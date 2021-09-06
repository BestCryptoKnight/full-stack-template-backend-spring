package com.example.fullstacktemplate.config.security.oauth2;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.service.*;
import com.example.fullstacktemplate.config.security.UserPrincipal;
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService.REDIRECT_URI_PARAM_COOKIE_NAME;

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
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException(messageService.getMessage("o2authInvalidTargetUrl"));
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        User user = userService.findById((((UserPrincipal) authentication.getPrincipal()).getId())).orElseThrow(()->new BadRequestException("userNotFound"));
        JwtToken refreshToken = authenticationService.createRefreshToken(user);
        response.addCookie(authenticationService.createRefreshTokenCookie(refreshToken.getValue(), (int) appProperties.getAuth().getPersistentTokenExpirationMsec()));
        String accessToken = authenticationService.createAccessToken(user);
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("expires", TimeUnit.MILLISECONDS.toDays(appProperties.getAuth().getPersistentTokenExpirationMsec()))
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken.getValue())
                .build().toUriString();
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
