package com.recommendationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendationservice.dto.AIRecommendationCandidate;
import com.recommendationservice.dto.PostResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIRecommendationService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public List<String> rankPosts(
            String userId,
            List<PostResponseDTO> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<AIRecommendationCandidate> aiCandidates =
                candidates.stream()
                        .map(post ->
                                new AIRecommendationCandidate(
                                        post.getId(),
                                        post.getUserId(),
                                        post.getContent(),
                                        post.getLikeCount() == null
                                                ? 0
                                                : post.getLikeCount(),
                                        post.getCommentCount() == null
                                                ? 0
                                                : post.getCommentCount(),
                                        post.getShareCount() == null
                                                ? 0
                                                : post.getShareCount(),
                                        false,
                                        post.getCreatedAt() == null
                                                ? null
                                                : post.getCreatedAt().toString()
                                 )
                        )
                        .toList();

        String candidatesJson;

        try {
            candidatesJson =
                    objectMapper.writeValueAsString(
                            aiCandidates
                    );
        } catch (Exception e) {
            log.error("Failed to serialize recommendation candidates for user {}: {}", userId, e.getMessage());
            return List.of();
        }

        String prompt = """
                You are a recommendation ranking system for a social media application.

                Your job is to rank posts for a user.

                User ID:
                %s

                Candidate posts:
                %s

                Ranking rules:

                1. Prefer posts that are likely to be personally relevant.
                2. Prefer recent posts.
                3. Prefer posts with meaningful engagement.
                4. Prefer posts from users the user follows.
                5. Avoid repetitive or low-value content.
                6. Do not invent information that is not present in the candidates.
                7. Only select posts from the provided candidate list.

                Return ONLY a JSON array containing post IDs.

                Example:
                ["post-id-1", "post-id-2", "post-id-3"]

                Return at most 10 post IDs.
                """.formatted(
                userId,
                candidatesJson
        );
        System.out.println(prompt);
        try {
            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return List.of();
            }
            System.out.println(response);
            return objectMapper.readValue(
                    response,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            log.error("AI recommendation ranking call failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}