package com.apigateway.config;

import com.apigateway.config.SecurityContextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JwtAuthenticationManager authManager,
                                                         SecurityContextRepository contextRepo) {
        return http
                .csrf(csrf -> csrf.disable())
                .authenticationManager(authManager)
                .securityContextRepository(contextRepo)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // The WebSocket upgrade carries Authorization as an HTTP
                        // header only if the client explicitly attached it.
                        // The STOMP CONNECT frame then carries X-USER-ID, which
                        // the chat service validates. So we let /chat/** through
                        // the gateway without HTTP-level auth.
                        .pathMatchers("/chat/**", "/chat").permitAll()
                        .pathMatchers("/api/users/login", "/api/users/register","/api/users/username/**" , "/eureka/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ attach source
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173")); // only one entry
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
