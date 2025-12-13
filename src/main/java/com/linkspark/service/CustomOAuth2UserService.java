package com.linkspark.service;

import com.linkspark.model.enums.AuthProvider;
import com.linkspark.domain.User;
import com.linkspark.repository.UserRepository;
import com.linkspark.security.CustomUserPrincipal;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId =
                request.getClientRegistration().getRegistrationId();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from provider");
        }

        AuthProvider provider =
                registrationId.equals("google")
                        ? AuthProvider.GOOGLE
                        : AuthProvider.GITHUB;

        String providerId =
                registrationId.equals("google")
                        ? oAuth2User.getAttribute("sub")
                        : String.valueOf(oAuth2User.getAttribute("id"));

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getProvider() == AuthProvider.LOCAL) {
                        existing.setProvider(provider);
                        existing.setProviderId(providerId);
                        return userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name != null ? name : "User");
                    u.setProvider(provider);
                    u.setProviderId(providerId);
                    return userRepository.save(u);
                });

        return new CustomUserPrincipal(user, oAuth2User.getAttributes());
    }
}

