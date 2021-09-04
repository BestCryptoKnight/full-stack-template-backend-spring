package com.example.fullstacktemplate.config.security;

import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.service.CustomUserDetailsService;
import com.example.fullstacktemplate.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    protected ResourceBundleMessageSource messageSource;

    @Autowired
    protected  LocaleResolver acceptHeaderLocaleResolver;

    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = getAccessJwtFromRequest(request).orElse(null);
        if (jwt != null && jwtTokenService.validateToken(jwt)) {
            Long userId = jwtTokenService.getUserIdFromToken(jwt);
            UserDetails userDetails = customUserDetailsService.loadUserById(userId)
                    .orElseThrow(()->new BadRequestException("userNotFound"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> getAccessJwtFromRequest(HttpServletRequest request) {
        Optional<String> optionalToken = Optional.empty();
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7, bearerToken.length()));
        }
        return optionalToken;
    }
}
