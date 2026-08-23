package com.recommendationservice.service;

import com.recommendationservice.client.FollowerServiceClient;
import com.recommendationservice.client.InteractionServiceClient;
import com.recommendationservice.client.PostServiceClient;
import com.recommendationservice.dto.PostResponseDTO;
import com.recommendationservice.entity.RecommendationHistory;
import com.recommendationservice.entity.RecommendationItemType;
import com.recommendationservice.repo.RecommendationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final PostServiceClient postServiceClient;

    private final FollowerServiceClient followerServiceClient;

    private final InteractionServiceClient interactionServiceClient;

    private final RecommendationHistoryRepository historyRepository;


    public List<PostResponseDTO> getRecommendations(String userId) {

        /*
         * STEP 1
         * Get all posts from Post Service
         */

        List<PostResponseDTO> posts =
                postServiceClient.getAllPosts(userId);


        /*
         * STEP 2
         * Get all posts that this user has
         * liked or commented on
         */

        Set<String> interactedPostIds =
                new HashSet<>(
                        interactionServiceClient
                                .getInteractedPostIds(userId)
                );


        /*
         * STEP 3
         * Get users this user follows
         */

        Set<String> followingIds =
                new HashSet<>(
                        followerServiceClient
                                .getFollowingUserIds(userId)
                );


        /*
         * STEP 4
         * Get posts already recommended before
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


        /*
         * STEP 5
         * Remove posts we don't want to recommend
         */

        List<PostResponseDTO> candidates =
                posts.stream()

                        // Remove deleted posts
                        .filter(post ->
                                !Boolean.TRUE.equals(
                                        post.getIsDeleted()
                                )
                        )

                        // Remove user's own posts
                        .filter(post ->
                                !userId.equals(
                                        post.getUserId()
                                )
                        )

                        // Remove posts user already interacted with
                        .filter(post ->
                                !interactedPostIds.contains(
                                        post.getId()
                                )
                        )

                        // Remove posts already recommended
                        .filter(post ->
                                !recommendedPostIds.contains(
                                        post.getId()
                                )
                        )

                        .toList();


        /*
         * STEP 6
         * Score and rank candidates
         */

        List<PostResponseDTO> recommendations =
                candidates.stream()

                        .sorted(
                                Comparator
                                        .comparingInt(
                                                post ->
                                                        calculateScore(
                                                                (PostResponseDTO) post,
                                                                followingIds
                                                        )
                                        )
                                        .reversed()
                        )

                        .limit(10)

                        .toList();


        /*
         * STEP 7
         * Save recommendation history
         */

        saveRecommendationHistory(
                userId,
                recommendations
        );


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
                Optional.ofNullable(post.getLikeCount())
                        .orElse(0);

        score += Math.min(likes, 20);


        /*
         * COMMENT SCORE
         *
         * 2 points per comment
         */

        int comments =
                Optional.ofNullable(post.getCommentCount())
                        .orElse(0);

        score += comments * 2;


        /*
         * SHARE SCORE
         */

        int shares =
                Optional.ofNullable(post.getShareCount())
                        .orElse(0);

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