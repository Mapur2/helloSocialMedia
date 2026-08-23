package com.recommendationservice.controller;

import com.recommendationservice.dto.PostResponseDTO;
import com.recommendationservice.service.RecommendationService;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;


    @GetMapping
    public List<PostResponseDTO> getRecommendations(
            @RequestParam(value = "user", required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String xuserId) {

        String id = null;
        if (userId == null)
            id = xuserId;
        else
            id = userId;
        return recommendationService
                .getRecommendations(id);
    }
}