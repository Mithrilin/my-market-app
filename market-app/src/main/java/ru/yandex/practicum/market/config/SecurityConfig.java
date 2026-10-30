package ru.yandex.practicum.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        DelegatingServerLogoutHandler logoutHandler = new DelegatingServerLogoutHandler(
                new WebSessionServerLogoutHandler(),
                new SecurityContextServerLogoutHandler()
        );

        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/{id}").permitAll()
                        .pathMatchers("/login", "/logout", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyExchange().authenticated()
                )
                .formLogin(withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutHandler(logoutHandler)
                )
                .csrf(withDefaults())
                .anonymous(anonymous -> {})
                .build();
    }
}