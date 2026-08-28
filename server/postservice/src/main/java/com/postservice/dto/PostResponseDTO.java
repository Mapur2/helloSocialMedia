package com.postservice.dto;

import com.postservice.entity.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PostResponseDTO(
        String id,
        String content,
        String userId,
        List<Map<String, String>> media,
        Visibility visibility,
        Integer likeCount,
        Integer commentCount,
        Integer shareCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isDeleted,
        Boolean isLikedByUser,
        String userName,
        String userProfilePicture
)
{
    public PostResponseDTO {
        if (isLikedByUser == null) isLikedByUser = false;
        if (userName == null) isLikedByUser = null;
    }
}
