package com.chatservice.dto;

import com.chatservice.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomRequest {
    private String roomId;
    private RoomType roomType;
}
