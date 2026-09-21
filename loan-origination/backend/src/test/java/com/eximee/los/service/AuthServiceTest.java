package com.eximee.los.service;

import com.eximee.los.config.JwtUtil;
import com.eximee.los.domain.Role;
import com.eximee.los.domain.User;
import com.eximee.los.dto.AuthRequest;
import com.eximee.los.dto.AuthResponse;
import com.eximee.los.dto.RegisterRequest;
import com.eximee.los.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "john@example.com", "pass1234", "John Doe", "+12345678", Role.ROLE_APPLICANT
        );

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(any(UserDetails.class), anyMap())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals("john@example.com", response.user().email());
        assertEquals("John Doe", response.user().fullName());
        assertEquals(Role.ROLE_APPLICANT, response.user().role());
    }

    @Test
    void testRegisterDuplicateEmailThrows() {
        RegisterRequest request = new RegisterRequest(
                "john@example.com", "pass1234", "John Doe", "+12345678", Role.ROLE_APPLICANT
        );

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest request = new AuthRequest("john@example.com", "pass1234");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("john@example.com")
                .password("encodedPassword")
                .authorities("ROLE_APPLICANT")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        User user = new User("john@example.com", "encodedPassword", "John Doe", "+12345678", Role.ROLE_APPLICANT);
        user.setId(1L);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(UserDetails.class), anyMap())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals(1L, response.user().id());
    }
}
