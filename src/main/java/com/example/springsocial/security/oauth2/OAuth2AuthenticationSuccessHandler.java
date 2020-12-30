package com.example.springsocial.security.oauth2;

import com.example.springsocial.config.AppProperties;
import com.example.springsocial.exception.BadRequestException;
import com.example.springsocial.model.JwtToken;
import com.example.springsocial.model.TokenType;
import com.example.springsocial.model.User;
import com.example.springsocial.repository.TokenRepository;
import com.example.springsocial.repository.UserRepository;
import com.example.springsocial.security.JwtTokenProvider;
import com.example.springsocial.security.UserPrincipal;
import com.example.springsocial.service.UserService;
import com.example.springsocial.util.CookieUtils;
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

import static com.example.springsocial.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final ResourceBundleMessageSource messageSource;

    private final LocaleResolver localeResolver;

    private JwtTokenProvider jwtTokenProvider;

    private AppProperties appProperties;

    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    private final TokenRepository tokenRepository;

    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    OAuth2AuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider, AppProperties appProperties,
                                       HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository, UserService userService, UserRepository userRepository, LocaleResolver localeResolver, ResourceBundleMessageSource messageSource, TokenRepository tokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appProperties = appProperties;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.localeResolver = localeResolver;
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
            throw new BadRequestException(messageSource.getMessage("o2authInvalidTargetUrl", null, localeResolver.resolveLocale(request)));
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        User user = userRepository.findByEmail(((UserPrincipal) authentication.getPrincipal()).getEmail()).orElseThrow(() -> new IllegalStateException(messageSource.getMessage("userNotFound", null, localeResolver.resolveLocale(request))));
        String refreshTokenValue = jwtTokenProvider.createToken(user, Duration.of(appProperties.getAuth().getPersistentTokenExpirationMsec(), ChronoUnit.MILLIS));
        JwtToken refreshToken = tokenRepository.save(userService.createToken(user, refreshTokenValue, TokenType.REFRESH));
        response.addCookie(userService.createRefreshTokenCookie(refreshToken.getValue(), (int) appProperties.getAuth().getPersistentTokenExpirationMsec()));
        String token = jwtTokenProvider.createToken((UserPrincipal) authentication.getPrincipal(), Duration.of(appProperties.getAuth().getAccessTokenExpirationMsec(), ChronoUnit.MILLIS));

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("expires", TimeUnit.MILLISECONDS.toDays(appProperties.getAuth().getPersistentTokenExpirationMsec()))
                .queryParam("access_token", token)
                .queryParam("refresh_token", refreshToken.getValue())
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
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
