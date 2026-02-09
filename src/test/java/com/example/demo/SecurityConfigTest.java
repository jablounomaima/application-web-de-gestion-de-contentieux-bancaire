package com.example.demo;

import com.example.demo.security.auth.AuthRequest;
import com.example.demo.security.auth.AuthResponse;
import com.example.demo.security.config.SecurityConfig;
import com.example.demo.security.controller.AuthController;
import com.example.demo.security.jwt.JwtAuthFilter;
import com.example.demo.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    // We mock AuthenticationManager to bypass actual auth logic and just verify the endpoint is reachable
    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    public void shouldAllowAccessToAuthLogin() throws Exception {
        // Prepare mock behavior
        UserDetails userDetails = new User("test", "test", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"test\", \"password\": \"test\"}"))
                .andExpect(status().isOk()); 
    }
}
