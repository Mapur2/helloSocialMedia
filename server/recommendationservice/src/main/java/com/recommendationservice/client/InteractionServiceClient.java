package com.recommendationservice.client;

import com.recommendationservice.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class InteractionServiceClient {

    @Autowired
    private RestTemplate restTemplate;
    @Value("${services.interaction.url}")
    private String interactionServiceUrl;

    public List<String> getInteractedPostIds(String userId) {

        String url =
                interactionServiceUrl
                        + "/api/post/interact/interactions"
                        + "?userId=" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<List<String>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<
                                ApiResponse<List<String>>
                                >() {
                        }
                );

        if (response.getBody() == null ||
                response.getBody().getData() == null) {

            return List.of();
        }

        return response.getBody().getData();
    }
}