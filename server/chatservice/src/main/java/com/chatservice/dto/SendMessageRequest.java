package com.chatservice.dto;

import com.chatservice.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {

    private String clientMessageId;

    private String senderId;

    private MessageType type;

    private String content;

    private String mediaId;
}