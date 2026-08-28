package com.recommendationservice.service;

import com.recommendationservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

    private final RecommendationProducer producer;
    private final UserServiceClient userServiceClient;

    @Scheduled(
            cron = "${recommendation.worker.cron}"
    )
    public void scheduleRecommendations() {
        log.info("Starting scheduled recommendation job generation...");

        List<String> users = userServiceClient.getActiveProfiles();

        if (users == null || users.isEmpty()) {
            log.warn("No active users found to generate recommendations for.");
            return;
        }

        log.info("Dispatching recommendation jobs for {} active users", users.size());

        for (String userId : users) {
            producer.sendRecommendationJob(userId);
        }

        log.info("Completed dispatching recommendation jobs.");
    }
}