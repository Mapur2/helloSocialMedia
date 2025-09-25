package com.userservice.service;

import com.userservice.config.JwtTokenProvider;
import com.userservice.dto.LoginDTO;
import com.userservice.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider provider;

    public boolean validateToken(String token){
        return provider.validateToken(token);
    }

    public LoginResponse login(LoginDTO l) {
        System.out.println("in auth service");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(l.getEmail(), l.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // generate token
        String token = provider.generateToken(authentication);

        MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();
        String userId = String.valueOf(userDetails.getId());

        return new LoginResponse(token, userId);
    }

}