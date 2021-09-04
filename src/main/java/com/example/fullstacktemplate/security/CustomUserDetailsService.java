package com.example.fullstacktemplate.security;


import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Optional;

/**
 * Created by rajeevkumarsingh on 02/08/17.
 */

@Service
public class CustomUserDetailsService implements UserDetailsService {

    final
    UserRepository userRepository;

    final
     LocaleResolver acceptHeaderLocaleResolver;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,  LocaleResolver acceptHeaderLocaleResolver) {
        this.userRepository = userRepository;
        this.acceptHeaderLocaleResolver = acceptHeaderLocaleResolver;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        return optionalUser.map(UserPrincipal::create).orElse(null);
    }

    @Transactional
    public Optional<UserDetails> loadUserById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.map(UserPrincipal::create);
    }
}
