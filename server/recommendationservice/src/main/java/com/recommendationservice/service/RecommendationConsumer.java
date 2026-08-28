package com.recommendationservice.service;

import com.recommendationservice.dto.PostResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationConsumer {

    private final RecommendationService recommendationService;

    @RabbitListener(queues = "recommendation.queue")
    public void processRecommendation(String userId) {
        log.info("Processing recommendation job for user: {}", userId);

        try {
            List<PostResponseDTO> recommendations = recommendationService.generateRecommendations(userId);
            log.info("Successfully generated and saved {} recommendations for user: {}", recommendations.size(), userId);
        } catch (Exception e) {
            log.error("Failed to generate recommendations for user {}: {}", userId, e.getMessage(), e);
        }
    }
}