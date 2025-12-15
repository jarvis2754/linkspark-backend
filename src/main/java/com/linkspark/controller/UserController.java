package com.linkspark.controller;

import com.linkspark.domain.User;
import com.linkspark.dto.ChangePasswordRequest;
import com.linkspark.dto.UpdateProfileRequest;
import com.linkspark.dto.UserProfileDto;
import com.linkspark.model.enums.AuthProvider;
import com.linkspark.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public UserProfileDto me(Authentication auth) {
        User user = (User) auth.getPrincipal();

        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getCompany(),
                user.getEmail(),
                user.getProvider().name()
        );
    }

    @PutMapping("/me")
    public UserProfileDto updateProfile(
            Authentication auth,
            @RequestBody UpdateProfileRequest req
    ) {
        User user = (User) auth.getPrincipal();

        user.setName(req.name());
        user.setCompany(req.company());

        userRepository.save(user);

        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getCompany(),
                user.getEmail(),
                user.getProvider().name()
        );
    }


    @PutMapping("/me/password")
    public void changePassword(
            Authentication auth,
            @RequestBody ChangePasswordRequest req
    ) {
        User user = (User) auth.getPrincipal();

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("OAuth users cannot change password");
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Wrong password");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}

