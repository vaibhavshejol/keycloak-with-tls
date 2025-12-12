package com.example.auth_service.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.example.auth_service.exception.CustomAccessDeniedHandler;
import com.example.auth_service.exception.CustomAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    
    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/meta-info**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/check/**").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                                
                .exceptionHandling(ex -> ex 
                         .authenticationEntryPoint(customAuthenticationEntryPoint)
                         .accessDeniedHandler(customAccessDeniedHandler));
                    
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<String> realmRoles = realmAccess != null
                    ? (Collection<String>) realmAccess.get("roles")
                    : List.of();

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Collection<String> resourceRoles = List.of();

            if (resourceAccess != null) {

                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("angular-app1");
                if (clientAccess != null) {
                    resourceRoles = (Collection<String>) clientAccess.get("roles");
                }
            }

            List<String> keycloakDefaultRoles = List.of(
                    "offline_access",
                    "uma_authorization",
                    "default-roles-master",
                    "default-roles-demo-realm");

            return Stream.concat(
                    realmRoles.stream(),
                    resourceRoles != null ? resourceRoles.stream() : Stream.empty())
                    .filter(role -> !keycloakDefaultRoles.contains(role))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toSet());
        };
    }
}
