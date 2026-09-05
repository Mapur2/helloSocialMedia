package com.chatservice.dto;

import com.chatservice.entity.MessageStatus;
import com.chatservice.entity.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private String id;

    private String clientMessageId;

    private String senderId;

    private String receiverId;

    private MessageType type;

    private String content;

    private String mediaId;

    private LocalDateTime timestamp;

    private MessageStatus status;
}