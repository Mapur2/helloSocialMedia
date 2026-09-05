package com.chatservice.dto;

import com.chatservice.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private String id;
    private String roomId;
    private RoomType roomType;
    private String name;
    private String createdBy;
    private List<String> memberIds;
    private String otherUserId; // Participant user ID for ONE_TO_ONE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
