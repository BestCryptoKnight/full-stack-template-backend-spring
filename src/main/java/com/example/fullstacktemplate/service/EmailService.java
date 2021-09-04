package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.config.AppProperties;
import com.example.fullstacktemplate.dto.ForgottenPasswordRequestDto;
import com.example.fullstacktemplate.dto.SignUpRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

@Component
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    private final AppProperties appProperties;
    private final MessageService messageService;

    @Autowired
    public EmailService(JavaMailSender emailSender, AppProperties appProperties, MessageService messageService) {
        this.emailSender = emailSender;
        this.appProperties = appProperties;
        this.messageService = messageService;
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

    public void sendAccountActivationMessage(SignUpRequestDto signUpRequestDto, String token) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getAccountActivationUri())
                .addParameter("token", token);
        this.sendSimpleMessage(
                signUpRequestDto.getEmail(),
                appProperties.getAppName() + " " + messageService.getMessage("activateAccountEmailSubject"),
                String.format("%s %s", messageService.getMessage("activateAccountEmailBody"), uriBuilder.build().toURL().toString())
        );
    }

    public void sendEmailChangeConfirmationMessage(String newEmail, String oldEmail, String token) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getEmailChangeConfirmationUri())
                .addParameter("token", token);
        this.sendSimpleMessage(
                newEmail,
                messageService.getMessage("confirmAccountEmailChangeEmailSubject", new Object[]{appProperties.getAppName()}),
                messageService.getMessage("confirmAccountEmailChangeEmailBody", new Object[]{oldEmail, newEmail, uriBuilder.build().toURL().toString()})
        );
    }

    public void sendPasswordResetMessage(ForgottenPasswordRequestDto forgottenPasswordRequestDto, String token) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(appProperties.getPasswordResetUri())
                .addParameter("email", forgottenPasswordRequestDto.getEmail())
                .addParameter("token", token);
        this.sendSimpleMessage(
                forgottenPasswordRequestDto.getEmail(),
                appProperties.getAppName() + " " + messageService.getMessage("passwordResetEmailSubject"),
                String.format("%s %s", messageService.getMessage("passwordResetEmailBody"), uriBuilder.build().toURL().toString())
        );
    }
}
