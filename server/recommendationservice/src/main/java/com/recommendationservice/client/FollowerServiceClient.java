package com.recommendationservice.client;

import com.recommendationservice.dto.ApiResponse;
import com.recommendationservice.dto.UserDTO;
import com.recommendationservice.dto.UserFollowerListDTO;
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
public class FollowerServiceClient {

    @Autowired
    private RestTemplate restTemplate;
    @Value("${services.follower.url}")
    private String followerServiceUrl ;

    public List<String> getFollowingUserIds(String userId) {

        String url =
                followerServiceUrl
                        + "/api/followers/following"
                        + "?user=" + userId;


        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<UserFollowerListDTO>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<
                                ApiResponse<UserFollowerListDTO>
                                >() {
                        }
                );

        if (response.getBody() == null ||
                response.getBody().getData() == null ||
                response.getBody().getData().getFollowers() == null) {

            return List.of();
        }

        return response.getBody()
                .getData()
                .getFollowers()
                .stream()
                .map(UserDTO::getId)
                .toList();
    }
}