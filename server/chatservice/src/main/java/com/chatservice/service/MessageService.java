package com.chatservice.service;

import com.chatservice.dto.SendMessageRequest;
import com.chatservice.entity.Message;
import com.chatservice.entity.MessageStatus;
import com.chatservice.entity.MessageType;
import com.chatservice.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepo;

    /**
     * Idempotent insert: if the same (senderId, clientMessageId) already
     * exists we return the stored row. Concurrent retries race against the
     * composite unique constraint; the loser catches the violation and
     * re-reads the existing row.
     */
    public Message saveMessage(SendMessageRequest chatMessage, String roomId) {
        MessageType type = chatMessage.getType() != null ? chatMessage.getType() : MessageType.TEXT;

        if (type == MessageType.IMAGE) {
            if (chatMessage.getMediaId() == null || chatMessage.getMediaId().isBlank()) {
                throw new IllegalArgumentException("mediaId is required for IMAGE message type");
            }
        } else if (type == MessageType.TEXT) {
            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("content is required for TEXT message type");
            }
        }

        // Fast-path idempotency check
        if (chatMessage.getClientMessageId() != null && !chatMessage.getClientMessageId().isBlank()) {
            Optional<Message> existing = messageRepo.findBySenderIdAndClientMessageId(
                    chatMessage.getSenderId(),
                    chatMessage.getClientMessageId()
            );
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Message message = Message.builder()
                .roomId(roomId)
                .clientMessageId(chatMessage.getClientMessageId())
                .senderId(chatMessage.getSenderId())
                .type(type)
                .content(chatMessage.getContent())
                .mediaId(chatMessage.getMediaId())
                .timestamp(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        try {
            return messageRepo.save(message);
        } catch (DataIntegrityViolationException e) {
            // Race: another concurrent request inserted with the same
            // (senderId, clientMessageId). Return the winning row.
            return messageRepo.findBySenderIdAndClientMessageId(
                    chatMessage.getSenderId(),
                    chatMessage.getClientMessageId()
            ).orElseThrow(() -> e);
        }
    }

    public Message updateMessageStatus(String messageId, MessageStatus newStatus) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Monotonic guard: SENT -> DELIVERED -> READ only.
        if (newStatus.ordinal() > message.getStatus().ordinal()) {
            message.setStatus(newStatus);
            return messageRepo.save(message);
        }
        return message;
    }

    public List<Message> markRoomMessagesAsRead(String roomId, String currentUserId) {
        List<Message> unreadMessages = messageRepo.findByRoomIdAndSenderIdNotAndStatusNot(
                roomId,
                currentUserId,
                MessageStatus.READ
        );
        if (unreadMessages.isEmpty()) {
            return unreadMessages;
        }
        unreadMessages.forEach(msg -> msg.setStatus(MessageStatus.READ));
        return messageRepo.saveAll(unreadMessages);
    }
}
