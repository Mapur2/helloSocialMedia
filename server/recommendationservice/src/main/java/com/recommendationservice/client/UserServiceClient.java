package com.recommendationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class UserServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.user.url}")
    private String userServiceUrl;

    public List<String> getActiveProfiles() {
        String url = userServiceUrl + "/api/users/active-users";

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            if (response.getBody() == null) {
                return List.of();
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch active profiles from User Service: {}", e.getMessage());
            return List.of();
        }
    }
}
