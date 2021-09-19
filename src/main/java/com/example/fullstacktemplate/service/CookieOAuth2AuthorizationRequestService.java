package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.util.CookieUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class CookieOAuth2AuthorizationRequestService implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    public static final String LANGUAGE_COOKIE_NAME = "language";
    public static final String TWO_FACTOR_CODE = "two_factor_code";
    public static final String RECOVERY_CODE = "recovery_code";
    private static final int cookieExpireSeconds = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, LANGUAGE_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, TWO_FACTOR_CODE);
            CookieUtils.deleteCookie(request, response, RECOVERY_CODE);
            return;
        }

        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtils.serialize(authorizationRequest), cookieExpireSeconds);
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        String language = request.getParameter(LANGUAGE_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, cookieExpireSeconds);
        }
        if (StringUtils.isNotBlank(language)) {
            CookieUtils.addCookie(response, LANGUAGE_COOKIE_NAME, language, cookieExpireSeconds);
        } else {
            CookieUtils.addCookie(response, LANGUAGE_COOKIE_NAME, "en", cookieExpireSeconds);
        }
        String twoFactorCode = request.getParameter(TWO_FACTOR_CODE);
        if (StringUtils.isNotBlank(twoFactorCode)) {
            CookieUtils.addCookie(response, TWO_FACTOR_CODE, twoFactorCode, cookieExpireSeconds);
        }

        String recoveryCode = request.getParameter(RECOVERY_CODE);
        if (StringUtils.isNotBlank(recoveryCode)) {
            CookieUtils.addCookie(response, RECOVERY_CODE, recoveryCode, cookieExpireSeconds);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, LANGUAGE_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, TWO_FACTOR_CODE);
        CookieUtils.deleteCookie(request, response, RECOVERY_CODE);
    }
}
