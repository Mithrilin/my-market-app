package ru.yandex.practicum.payment.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NestedResourceAccessRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    public static final String CLIENT_ID = "market-app";
    public static final String ROLES_CLAIM = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return List.of();
        }

        Object clientAccessObj = resourceAccess.get(CLIENT_ID);
        if (!(clientAccessObj instanceof Map<?, ?> clientAccess)) {
            return List.of();
        }

        Object rolesObj = clientAccess.get(ROLES_CLAIM);
        if (!(rolesObj instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }
}