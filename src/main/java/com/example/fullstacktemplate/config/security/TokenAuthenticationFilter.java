package com.example.fullstacktemplate.config.security;

import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.service.CustomUserDetailsService;
import com.example.fullstacktemplate.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class TokenAuthenticationFilter extends OncePerRequestFilter {


    private final TokenService tokenService;
    private final  CustomUserDetailsService customUserDetailsService;

    public TokenAuthenticationFilter(TokenService tokenService, CustomUserDetailsService customUserDetailsService) {
        this.tokenService = tokenService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = getAccessJwtFromRequest(request).orElse(null);
        if (jwt != null && tokenService.validateJwtToken(jwt)) {
            Long userId = tokenService.getUserIdFromToken(jwt);
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
