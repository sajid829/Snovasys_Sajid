package com.snovasys.ledgerline.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snovasys.ledgerline.dto.ApiErrorResponse;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accounts/**").hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.POST, "/api/transfers").hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.GET, "/api/accounts/**")
                        .hasAnyRole("ADMIN", "ACCOUNT_HOLDER", "AUDITOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(
                                        objectMapper,
                                        response,
                                        401,
                                        "Unauthorized",
                                        "Authentication is required or the access token is invalid.",
                                        request.getRequestURI())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(
                                        objectMapper,
                                        response,
                                        401,
                                        "Unauthorized",
                                        "Authentication is required.",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(
                                        objectMapper,
                                        response,
                                        403,
                                        "Forbidden",
                                        "You are not allowed to perform this operation.",
                                        request.getRequestURI())))
                .build();
    }

    private void writeSecurityError(
            ObjectMapper objectMapper,
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String error,
            String message,
            String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(Instant.now(), status, error, message, path));
    }
}
