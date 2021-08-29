package com.example.fullstacktemplate.service;


import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Optional;

@Service
public class MessageService {
    private final LocaleResolver localeResolver;
    private final ResourceBundleMessageSource messageSource;

    public MessageService(LocaleResolver localeResolver, ResourceBundleMessageSource messageSource) {
        this.localeResolver = localeResolver;
        this.messageSource = messageSource;
    }

    private Locale getLocaleForCurrentRequest() {
        return Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .map(ServletRequestAttributes::getRequest)
                .map(localeResolver::resolveLocale)
                .orElse(Locale.getDefault());
    }

    public final String getMessage(String code) {
        return getMessage(code,null);
    }

    public final String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
        return getMessage(code,args,null);
    }

    public final String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage) {
        Locale locale = getLocaleForCurrentRequest();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }


}
