package com.example.fullstacktemplate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class i18nConfig {
    @Bean
    public ResourceBundleMessageSource messageSource(){
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasename("lang/res");
        resourceBundleMessageSource.setDefaultEncoding(String.valueOf(StandardCharsets.UTF_8));
        resourceBundleMessageSource.setDefaultLocale(Locale.ENGLISH);
        resourceBundleMessageSource.setUseCodeAsDefaultMessage(true);
        return resourceBundleMessageSource;
    }
}
