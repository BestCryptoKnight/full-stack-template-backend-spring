package com.example.springsocial.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class JwtTokenAuthenticationFilterTest {


    @Autowired
    private TokenAuthenticationFilter tokenAuthenticationFilter;


    @Test
    @Disabled
    public void test() throws Exception {
        tokenAuthenticationFilter.doFilterInternal(null, null, null);
    }

}
