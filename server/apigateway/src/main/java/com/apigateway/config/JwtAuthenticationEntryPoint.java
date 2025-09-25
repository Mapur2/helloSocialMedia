package com.apigateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = ("{\"Unauthorized\": \"" + ex.getMessage()+"\"}").getBytes(StandardCharsets.UTF_8);
        var buffer = response.bufferFactory().wrap(bytes);
        response.getHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }
}
