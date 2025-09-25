package com.apigateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Collections;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private static final String jwtSecret = "KbZgx+9oVJ4PvW4byHz7rZ2EyRHg1mHGzVCPOBAvkmI=";

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        try {
            String username = Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject(); // usually the email or userId

            return Mono.just(new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList()));
        } catch (Exception e) {
            return Mono.empty(); // will cause 401 Unauthorized
        }
    }

    public static SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // decode from base64 if stored as base64
        return Keys.hmacShaKeyFor(keyBytes);
    }



    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
