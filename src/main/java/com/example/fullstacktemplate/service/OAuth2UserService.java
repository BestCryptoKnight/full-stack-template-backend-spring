package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.exception.OAuth2AuthenticationProcessingException;
import com.example.fullstacktemplate.model.*;
import com.example.fullstacktemplate.repository.UserRepository;
import com.example.fullstacktemplate.config.security.UserPrincipal;
import com.example.fullstacktemplate.config.security.oauth2.user.OAuth2UserInfo;
import com.example.fullstacktemplate.config.security.oauth2.user.OAuth2UserInfoFactory;
import com.example.fullstacktemplate.service.FileDbService;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

@Service
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileDbService fileDbService;

    @Autowired
    private SecretGenerator twoFactorSecretGenerator;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error while authentication oauth2 user",ex);
            throw new InternalAuthenticationServiceException("somethingWrong");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) throws IOException {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        if (StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("emailNotFoundFromO2Auth");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                throw new OAuth2AuthenticationProcessingException("alreadyHaveAccountO2AuthTemplate");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) throws IOException {
        User user = new User();
        user.setEmailVerified(true);
        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setTwoFactorSecret(twoFactorSecretGenerator.generate());
        user.setTwoFactorEnabled(false);
        user.setRole(Role.USER);
        URL url = new URL(oAuth2UserInfo.getImageUrl());
        FileDb profileImage = new FileDb("profile_image.png", FileType.fromMimeType(url.openConnection().getContentType()).orElse(FileType.IMAGE_PNG), IOUtils.toByteArray(url));
        user.setProfileImage(profileImage);
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) throws IOException {
        existingUser.setName(oAuth2UserInfo.getName());
        URL url = new URL(oAuth2UserInfo.getImageUrl());
        existingUser.setProfileImage(fileDbService.save("profile_image.png",  FileType.fromMimeType(url.openConnection().getContentType()).orElse(FileType.IMAGE_PNG), IOUtils.toByteArray(url)));
        return userRepository.save(existingUser);
    }

}
