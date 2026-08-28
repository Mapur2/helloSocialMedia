package com.recommendationservice.controller;

import com.recommendationservice.dto.ApiResponse;
import com.recommendationservice.dto.PostResponseDTO;
import com.recommendationservice.service.RecommendationScheduler;
import com.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/recommendations", "/recommendation"})
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationScheduler recommendationScheduler;
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDTO>>> getRecommendations(
            @RequestParam(value = "user", required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String xuserId) {

        String targetUserId = (userId != null && !userId.isBlank()) ? userId : xuserId;

        if (targetUserId == null || targetUserId.isBlank()) {
            log.warn("getRecommendations called without user ID in request parameter or 'X-User-Id' header");
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "User ID is required (via 'user' query param or 'X-User-Id' header)", List.of())
            );
        }

        try {
            log.info("Fetching recommendations for user: {}", targetUserId);
            List<PostResponseDTO> recommendations = recommendationService.getRecommendations(targetUserId);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Recommendations fetched successfully", recommendations)
            );
        } catch (Exception e) {
            log.error("Failed to fetch recommendations for user {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(false, "Failed to fetch recommendations: " + e.getMessage(), List.of())
            );
        }
    }


    @GetMapping("/trigger")
    public ResponseEntity<ApiResponse<String>> triggerGenerateRecommendations(){
        new Thread(() -> {
            recommendationScheduler.scheduleRecommendations();
        }).start();
        return ResponseEntity.ok(new ApiResponse<>(true, "Recommendation Triggered", null));
    }
}