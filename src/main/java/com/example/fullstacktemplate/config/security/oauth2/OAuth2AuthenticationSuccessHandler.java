package com.example.fullstacktemplate.config.security.oauth2;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.JwtToken;
import com.example.fullstacktemplate.model.TokenType;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.TokenRepository;
import com.example.fullstacktemplate.repository.UserRepository;
import com.example.fullstacktemplate.service.CookieOAuth2AuthorizationRequestService;
import com.example.fullstacktemplate.service.JwtTokenService;
import com.example.fullstacktemplate.config.security.UserPrincipal;
import com.example.fullstacktemplate.service.UserService;
import com.example.fullstacktemplate.util.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
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
    private final ResourceBundleMessageSource messageSource;

    private final  LocaleResolver acceptHeaderLocaleResolver;

    private JwtTokenService jwtTokenService;

    private AppProperties appProperties;

    private CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService;

    private final TokenRepository tokenRepository;

    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    OAuth2AuthenticationSuccessHandler(JwtTokenService jwtTokenService, AppProperties appProperties,
                                       CookieOAuth2AuthorizationRequestService cookieOAuth2AuthorizationRequestService, UserService userService, UserRepository userRepository, LocaleResolver acceptHeaderLocaleResolver, ResourceBundleMessageSource messageSource, TokenRepository tokenRepository) {
        this.jwtTokenService = jwtTokenService;
        this.appProperties = appProperties;
        this.cookieOAuth2AuthorizationRequestService = cookieOAuth2AuthorizationRequestService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.acceptHeaderLocaleResolver = acceptHeaderLocaleResolver;
        this.messageSource = messageSource;
        this.tokenRepository = tokenRepository;
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
            throw new BadRequestException(messageSource.getMessage("o2authInvalidTargetUrl", null, acceptHeaderLocaleResolver.resolveLocale(request)));
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        User user = userService.findById((((UserPrincipal) authentication.getPrincipal()).getId())).orElseThrow(()->new BadRequestException("userNotFound"));
        String refreshTokenValue = jwtTokenService.createTokenValue(user.getId(), Duration.of(appProperties.getAuth().getPersistentTokenExpirationMsec(), ChronoUnit.MILLIS));
        JwtToken refreshToken = tokenRepository.save(userService.createToken(user, refreshTokenValue, TokenType.REFRESH));
        response.addCookie(userService.createRefreshTokenCookie(refreshToken.getValue(), (int) appProperties.getAuth().getPersistentTokenExpirationMsec()));
        String token = jwtTokenService.createTokenValue(((UserPrincipal) authentication.getPrincipal()).getId(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("expires", TimeUnit.MILLISECONDS.toDays(appProperties.getAuth().getPersistentTokenExpirationMsec()))
                .queryParam("access_token", token)
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
