package com.recommendationservice.client;

import com.recommendationservice.dto.ApiResponse;
import com.recommendationservice.dto.PostResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class PostServiceClient {

    @Autowired
    private RestTemplate restTemplate;
    @Value("${services.post.url}")
    private String postServiceUrl;

    public List<PostResponseDTO> getAllPosts(String userId) {

        String url = postServiceUrl + "/api/posts";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<List<PostResponseDTO>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<
                                ApiResponse<List<PostResponseDTO>>
                                >() {}
                );

        if (response.getBody() == null ||
                response.getBody().getData() == null) {

            return List.of();
        }

        return response.getBody().getData();
    }
}