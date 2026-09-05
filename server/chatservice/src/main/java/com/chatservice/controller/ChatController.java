package com.chatservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chatservice.config.SessionRegistry;
import com.chatservice.dto.Response;
import com.chatservice.dto.SendMessageRequest;
import com.chatservice.entity.Message;
import com.chatservice.service.MessageService;
import com.chatservice.service.RoomService;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SessionRegistry sessionRegistry;

    /**
     * Inbound:  /app/send-message/{roomId}
     * Outbound: /topic/room/{roomId}
     *
     * Membership is already enforced by MembershipInterceptor on the SEND
     * and SUBSCRIBE frames, so we can broadcast on a plain /topic destination
     * without leaking to non-members.
     */
    @MessageMapping("/send-message/{roomId}")
    public void sendPrivateMessage(
            @Payload SendMessageRequest chatMessage,
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            String senderId = resolveUserId(headerAccessor);
            if (senderId == null) {
                return;
            }
            chatMessage.setSenderId(senderId);

            Message message = roomService.sendMessageToRoom(chatMessage, roomId);
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    new Response<>(true, "Sent message", message)
            );
        } catch (Exception e) {
            Response<?> err = new Response<>(false, e.getMessage(), null);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/error", err);
        }
    }

    /**
     * Status updates (DELIVERED / READ). Broadcasts to /topic/room/{roomId}/status.
     */
    @MessageMapping("/message-status/{roomId}")
    public void updateStatus(
            @Payload com.chatservice.dto.MessageStatusUpdateRequest statusRequest,
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            String senderId = resolveUserId(headerAccessor);
            if (senderId == null) {
                return;
            }
            Message message = messageService.updateMessageStatus(
                    statusRequest.getMessageId(),
                    statusRequest.getStatus()
            );
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/status",
                    new Response<>(true, "Status updated", message)
            );
        } catch (Exception e) {
            Response<?> err = new Response<>(false, e.getMessage(), null);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/error", err);
        }
    }

    /**
     * Spring's @MessageMapping principal resolution relies on
     * SimpUserRegistry, which never knew about our session-anchored user.
     * We resolve the user id directly from the SessionRegistry via the
     * session id Spring assigns on the WebSocket session.
     */
    private String resolveUserId(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) return null;
        String sessionId = headerAccessor.getSessionId();
        return sessionRegistry.lookup(sessionId);
    }
}
