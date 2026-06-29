package com.dis.workshopticketing.identityservice.security;

import com.dis.workshopticketing.identityservice.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final OAuth2IdentityService oAuth2IdentityService;

    public CustomOAuth2UserService(OAuth2IdentityService oAuth2IdentityService) {
        this.oAuth2IdentityService = oAuth2IdentityService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

        OAuth2UserProfile profile = extractProfile(registrationId, attributes);
        User user = oAuth2IdentityService.upsertOAuth2User(
                registrationId,
                profile.providerId(),
                profile.email(),
                profile.firstName(),
                profile.lastName()
        );

        attributes.put("localUserId", user.getId());
        attributes.put("localEmail", user.getEmail());
        attributes.put("localRole", user.getRole().name());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                profile.nameAttributeKey()
        );
    }

    private OAuth2UserProfile extractProfile(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new OAuth2UserProfile(
                    "sub",
                    value(attributes, "sub"),
                    value(attributes, "email"),
                    value(attributes, "given_name"),
                    fallback(value(attributes, "family_name"), "-")
            );
        }

        if ("github".equals(registrationId)) {
            String name = fallback(value(attributes, "name"), value(attributes, "login"));
            String[] parts = name.split(" ", 2);
            return new OAuth2UserProfile(
                    "id",
                    value(attributes, "id"),
                    value(attributes, "email"),
                    parts[0],
                    parts.length > 1 ? parts[1] : "-"
            );
        }

        throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
    }

    private String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new OAuth2AuthenticationException("OAuth2 provider did not return required field: " + key);
        }
        return value.toString();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record OAuth2UserProfile(
            String nameAttributeKey,
            String providerId,
            String email,
            String firstName,
            String lastName
    ) {
    }
}
