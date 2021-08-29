package com.example.fullstacktemplate.config;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public  class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    public RequestSizeLimitFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getContentLengthLong() > appProperties.getMaxRequestSize()) {
            throw new IOException();
        }
        filterChain.doFilter(request, response);
    }

}
