package com.recommendationservice.service;

import com.recommendationservice.client.FollowerServiceClient;
import com.recommendationservice.client.InteractionServiceClient;
import com.recommendationservice.client.PostServiceClient;
import com.recommendationservice.dto.PostResponseDTO;
import com.recommendationservice.entity.RecommendationHistory;
import com.recommendationservice.entity.RecommendationItemType;
import com.recommendationservice.repo.RecommendationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final PostServiceClient postServiceClient;
    private final FollowerServiceClient followerServiceClient;
    private final InteractionServiceClient interactionServiceClient;
    private final RecommendationHistoryRepository historyRepository;
    private final AIRecommendationService aiRecommendationService;


    /**
     * Called by the REST API endpoint (/api/recommendations).
     * Retrieves pre-computed, stored recommendations without re-running the heavy LLM generation pipeline.
     * Falls back to on-demand generation only if no stored recommendations exist yet.
     */
    public List<PostResponseDTO> getRecommendations(String userId) {
        List<RecommendationHistory> recentHistory = historyRepository
                .findByUserIdAndItemTypeOrderByCreatedAtDesc(userId, RecommendationItemType.POST);

        if (recentHistory != null && !recentHistory.isEmpty()) {
            log.info("[User: {}] Found {} stored recommendations in history. Returning cached recommendations.", userId, recentHistory.size());

            // Take the top 10 most recent recommended post IDs (preserve order and distinct)
            List<String> postIds = recentHistory.stream()
                    .map(RecommendationHistory::getItemId)
                    .distinct()
                    .limit(10)
                    .toList();

            // Fetch current posts from Post Service to hydrate full post DTOs
            List<PostResponseDTO> allPosts = postServiceClient.getAllPosts(userId);
            Map<String, PostResponseDTO> postMap = allPosts.stream()
                    .collect(Collectors.toMap(PostResponseDTO::getId, p -> p, (p1, p2) -> p1));

            List<PostResponseDTO> hydratedRecommendations = postIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .filter(post -> !Boolean.TRUE.equals(post.getIsDeleted()))
                    .toList();

            if (!hydratedRecommendations.isEmpty()) {
                return hydratedRecommendations;
            }
        }

        // If no stored recommendations exist yet (e.g. new user), generate them on-demand
        log.info("[User: {}] No stored recommendations found, generating on-demand...", userId);
        return generateRecommendations(userId);
    }

    /**
     * Called by the background message queue worker (RecommendationConsumer).
     * Computes candidate posts, filters, applies AI ranking, and stores results in RecommendationHistory.
     */
    public List<PostResponseDTO> generateRecommendations(String userId) {

        /*
         * 1. Get candidate posts
         */
        List<PostResponseDTO> posts =
                postServiceClient.getRecommendationCandidates(userId);

        log.info("[User: {}] Fetched {} candidate posts from Post Service", userId, posts.size());

        /*
         * 2. Get posts already interacted with
         */
        Set<String> interactedPostIds =
                new HashSet<>(
                        interactionServiceClient
                                .getInteractedPostIds(userId)
                );

        log.info("[User: {}] User interacted with {} posts", userId, interactedPostIds.size());

        /*
         * 3. Get users this user follows
         */
        Set<String> followingIds =
                new HashSet<>(
                        followerServiceClient
                                .getFollowingUserIds(userId)
                );

        /*
         * 4. Get posts already recommended previously
         */
        Set<String> recommendedPostIds =
                historyRepository
                        .findByUserIdAndItemType(
                                userId,
                                RecommendationItemType.POST
                        )
                        .stream()
                        .map(RecommendationHistory::getItemId)
                        .collect(Collectors.toSet());

        log.info("[User: {}] User already has {} previously recommended posts in history", userId, recommendedPostIds.size());

        /*
         * 5. Filter candidates
         */
        List<PostResponseDTO> candidates =
                posts.stream()

                        // Don't recommend deleted posts
                        .filter(post ->
                                !Boolean.TRUE.equals(
                                        post.getIsDeleted()
                                )
                        )

                        // Don't recommend user's own posts
                        .filter(post ->
                                !userId.equals(
                                        post.getUserId()
                                )
                        )

                        // Don't recommend posts already interacted with
                        .filter(post ->
                                !interactedPostIds.contains(
                                        post.getId()
                                )
                        )

                        // Don't recommend posts already recommended
                        .filter(post ->
                                !recommendedPostIds.contains(
                                        post.getId()
                                )
                        )

                        .toList();

        log.info("[User: {}] Candidates remaining after filtering: {}", userId, candidates.size());

        if (candidates.isEmpty()) {
            log.info("[User: {}] No eligible candidates for LLM ranking", userId);
            return List.of();
        }

        /*
         * 6. Rule-based pre-ranking
         *
         * We don't send every post to the LLM.
         *
         * First select the top 30 candidates.
         */
        List<PostResponseDTO> recommendationsForLLM =
                candidates.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        post ->
                                                calculateScore(
                                                        (PostResponseDTO) post,
                                                        followingIds
                                                )
                                ).reversed()
                        )
                        .limit(30)
                        .toList();

        log.info("[User: {}] Sending {} candidates to LLM for ranking", userId, recommendationsForLLM.size());


        /*
         * 7. Send only the top 30 candidates to the LLM
         *
         * LLM returns ordered post IDs.
         */
        List<String> rankedPostIds =
                aiRecommendationService.rankPosts(
                        userId,
                        recommendationsForLLM
                );


        /*
         * 8. Convert returned IDs back to Post objects
         */
        Map<String, PostResponseDTO> postMap =
                candidates.stream()
                        .collect(
                                Collectors.toMap(
                                        PostResponseDTO::getId,
                                        post -> post
                                )
                        );


        /*
         * 9. Preserve the order returned by the LLM (or fallback to pre-ranked candidates)
         */
        List<PostResponseDTO> recommendations;
        if (rankedPostIds == null || rankedPostIds.isEmpty()) {
            recommendations = recommendationsForLLM.stream()
                    .limit(10)
                    .toList();
        } else {
            recommendations = rankedPostIds.stream()
                    // Make sure LLM didn't return an invalid ID
                    .map(postMap::get)
                    // Remove invalid/null IDs
                    .filter(Objects::nonNull)
                    // Only return top 10
                    .limit(10)
                    .toList();
        }


        /*
         * 10. Save recommendation history
         */
        saveRecommendationHistory(
                userId,
                recommendations
        );


        /*
         * 11. Return recommendations
         */
        return recommendations;
    }


    private int calculateScore(
            PostResponseDTO post,
            Set<String> followingIds
    ) {

        int score = 0;


        /*
         * FOLLOWING BONUS
         */
        if (followingIds.contains(post.getUserId())) {
            score += 50;
        }


        /*
         * LIKE SCORE
         *
         * Maximum 20 points
         */
        int likes =
                Optional.ofNullable(
                        post.getLikeCount()
                ).orElse(0);

        score += Math.min(likes, 20);


        /*
         * COMMENT SCORE
         */
        int comments =
                Optional.ofNullable(
                        post.getCommentCount()
                ).orElse(0);

        score += comments * 2;


        /*
         * SHARE SCORE
         */
        int shares =
                Optional.ofNullable(
                        post.getShareCount()
                ).orElse(0);

        score += shares * 3;


        /*
         * RECENCY SCORE
         */
        if (post.getCreatedAt() != null) {

            long hoursOld =
                    ChronoUnit.HOURS.between(
                            post.getCreatedAt(),
                            LocalDateTime.now()
                    );

            if (hoursOld < 1) {

                score += 20;

            } else if (hoursOld < 24) {

                score += 15;

            } else if (hoursOld < 72) {

                score += 10;

            } else if (hoursOld < 168) {

                score += 5;
            }
        }

        return score;
    }


    private void saveRecommendationHistory(
            String userId,
            List<PostResponseDTO> recommendations
    ) {

        if (recommendations.isEmpty()) {
            return;
        }

        List<RecommendationHistory> historyList =
                recommendations.stream()

                        .map(post -> {

                            RecommendationHistory history =
                                    new RecommendationHistory();

                            history.setUserId(userId);

                            history.setItemId(
                                    post.getId()
                            );

                            history.setItemType(
                                    RecommendationItemType.POST
                            );

                            history.setCreatedAt(
                                    LocalDateTime.now()
                            );

                            return history;
                        })

                        .toList();


        historyRepository.saveAll(historyList);
    }
}