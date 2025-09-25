package com.userservice.config;

import com.userservice.service.UserService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Autowired
    private UserService userService;
    //1 hour
    private long jwtExpires = 60*60*1000;
    private String jwtSecret = "KbZgx+9oVJ4PvW4byHz7rZ2EyRHg1mHGzVCPOBAvkmI=";

    public String getEmailId(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token){
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parse(token);
            return true;
        }
        catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByEmailId(username).getId();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpires);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(currentDate)
                .expiration(expireDate)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }


    private String generateSecretKey(){
        int length = 32;
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[length];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
