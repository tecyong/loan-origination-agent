package com.eximee.los.service;

import com.eximee.los.config.JwtUtil;
import com.eximee.los.domain.Role;
import com.eximee.los.domain.User;
import com.eximee.los.dto.AuthRequest;
import com.eximee.los.dto.AuthResponse;
import com.eximee.los.dto.RegisterRequest;
import com.eximee.los.dto.UserDto;
import com.eximee.los.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new IllegalArgumentException("User with email " + request.email() + " already exists.");
        }

        Role role = request.role() != null ? request.role() : Role.ROLE_APPLICANT;

        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber(),
                role
        );

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPasswordHash())
                .authorities(savedUser.getRole().name())
                .build();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", savedUser.getRole().name());
        extraClaims.put("fullName", savedUser.getFullName());
        extraClaims.put("userId", savedUser.getId());

        String token = jwtUtil.generateToken(userDetails, extraClaims);

        return new AuthResponse(
                token,
                new UserDto(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getPhoneNumber(), savedUser.getRole())
        );
    }

    public AuthResponse login(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase().trim(), request.password())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("userId", user.getId());

        String token = jwtUtil.generateToken(userDetails, extraClaims);

        return new AuthResponse(
                token,
                new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getPhoneNumber(), user.getRole())
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}
