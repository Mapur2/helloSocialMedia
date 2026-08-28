package com.recommendationservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostResponseDTO {

    private String id;

    private String userId;

    private String content;

    private List<Map<String, String>> media;

    private String visibility;

    private Integer likeCount;

    private Integer commentCount;

    private Integer shareCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isDeleted;

    private Boolean isLikedByUser;

    private String userName;

    private String userProfilePicture;
}