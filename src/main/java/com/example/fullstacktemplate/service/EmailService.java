package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.dto.ForgottenPasswordRequest;
import com.example.fullstacktemplate.dto.SignUpRequest;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public class EmailService {

    private final ResourceBundleMessageSource messageSource;
    private final JavaMailSender emailSender;
    private final AppProperties appProperties;

    @Autowired
    public EmailService(JavaMailSender emailSender, AppProperties appProperties, ResourceBundleMessageSource messageSource) {
        this.emailSender = emailSender;
        this.appProperties = appProperties;
        this.messageSource = messageSource;
    }

    public void sendSimpleMessage(
            String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@fullstack.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);

    }

    public void sendAccountActivationMessage(SignUpRequest signUpRequest, String token, Locale messageLocale) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getAccountActivationUri())
                .addParameter("token", token);
        this.sendSimpleMessage(
                signUpRequest.getEmail(),
                appProperties.getAppName() + " " + messageSource.getMessage("activateAccountEmailSubject", null, messageLocale),
                String.format("%s %s", messageSource.getMessage("activateAccountEmailBody", null, messageLocale), uriBuilder.build().toURL().toString())
        );
    }

    public void sendEmailChangeConfirmationMessage(String newEmail, String oldEmail, String token, Locale messageLocale) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getEmailChangeConfirmationUri())
                .addParameter("token", token);
        this.sendSimpleMessage(
                newEmail,
                messageSource.getMessage("confirmAccountEmailChangeEmailSubject", new Object[]{appProperties.getAppName()}, messageLocale),
                messageSource.getMessage("confirmAccountEmailChangeEmailBody", new Object[]{oldEmail, newEmail, uriBuilder.build().toURL().toString()}, messageLocale)
        );
    }

    public void sendPasswordResetMessage(ForgottenPasswordRequest forgottenPasswordRequest, String token, Locale messageLocale) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getPasswordResetUri())
                .addParameter("email", forgottenPasswordRequest.getEmail())
                .addParameter("token", token);
        this.sendSimpleMessage(
                forgottenPasswordRequest.getEmail(),
                appProperties.getAppName() + " " + messageSource.getMessage("passwordResetEmailSubject", null, messageLocale),
                String.format("%s %s", messageSource.getMessage("passwordResetEmailBody", null, messageLocale), uriBuilder.build().toURL().toString())
        );
    }
}
