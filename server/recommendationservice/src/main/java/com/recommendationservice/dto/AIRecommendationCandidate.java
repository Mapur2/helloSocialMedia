package com.recommendationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AIRecommendationCandidate {

    private String postId;

    private String authorId;

    private String content;

    private int likeCount;

    private int commentCount;

    private int shareCount;

    private boolean fromFollowing;

    private String createdAt;
}