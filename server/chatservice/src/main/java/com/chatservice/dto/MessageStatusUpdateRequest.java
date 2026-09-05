package com.chatservice.dto;

import com.chatservice.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateRequest {
    private String messageId;
    private String roomId;
    private MessageStatus status;
}
