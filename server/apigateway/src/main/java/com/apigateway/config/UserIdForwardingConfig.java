package com.apigateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;


import javax.crypto.SecretKey;

@Configuration
public class UserIdForwardingConfig {

    @Bean
    public GlobalFilter addUserIdHeaderFilter() {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            String rawToken = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                rawToken = authHeader.substring(7); // remove "Bearer " prefix

                // Parse JWT to get claims
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(rawToken)
                        .getPayload();

                String email = claims.getSubject();           // subject = email/username
                String userId = claims.get("userId", String.class); // custom claim

                // Forward as headers to downstream services
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.headers(headers -> {
                            headers.add("X-User-Id", userId);
                            headers.add("X-Username", email);
                        }))
                        .build();

                return chain.filter(mutatedExchange);
            }

            return chain.filter(exchange);
        };
    }

    // Replace with your actual JWT secret key
    private SecretKey getSigningKey() {
        return JwtAuthenticationManager.getKey(); // your existing key() method
    }
}
