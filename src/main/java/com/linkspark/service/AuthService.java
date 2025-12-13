package com.linkspark.service;

import com.linkspark.model.enums.AuthProvider;
import com.linkspark.domain.User;
import com.linkspark.dto.AuthDtos;
import com.linkspark.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // ---------------- SIGNUP (EMAIL/PASSWORD) ----------------

    @Transactional
    public AuthDtos.TokenResponse signup(AuthDtos.SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL); // 🔒 important

        userRepository.save(user);

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        return new AuthDtos.TokenResponse(access, refresh);
    }

    // ---------------- LOGIN (EMAIL/PASSWORD) ----------------

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // 🚫 BLOCK OAuth users from password login
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException(
                    "This account uses social login. Please sign in with Google/GitHub."
            );
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User principal = (User) authentication.getPrincipal();

        String access = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);

        return new AuthDtos.TokenResponse(access, refresh);
    }

    // ---------------- REFRESH TOKEN ----------------

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {

        String email = jwtService.extractUsername(request.refreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!jwtService.isValid(request.refreshToken(), user, "refresh")) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        return new AuthDtos.TokenResponse(access, refresh);
    }
}
