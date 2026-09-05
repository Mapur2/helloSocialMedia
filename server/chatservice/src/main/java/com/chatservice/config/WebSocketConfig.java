package com.chatservice.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.chatservice.repository.RoomMemberRepository;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private MembershipInterceptor membershipInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use /topic for room broadcasts. The UserDestinationMessageHandler
        // doesn't reliably route to /user/* destinations through the simple
        // broker when the user prefix is also /user, so we stick to /topic
        // and gate access with a membership interceptor below.
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new UserBindingInterceptor(sessionRegistry), membershipInterceptor);
    }
}

/**
 * Captures the X-USER-ID header on the CONNECT frame and stores it in the
 * SessionRegistry keyed by Spring's sessionId.
 */
class UserBindingInterceptor implements ChannelInterceptor {
    private final SessionRegistry sessionRegistry;

    UserBindingInterceptor(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == StompCommand.CONNECT) {
            String userId = accessor.getFirstNativeHeader("X-USER-ID");
            if (userId == null) userId = accessor.getFirstNativeHeader("X-User-Id");
            if (userId == null) userId = accessor.getFirstNativeHeader("userId");
            if (userId != null && !userId.isBlank()) {
                String sessionId = accessor.getSessionId();
                sessionRegistry.bind(sessionId, userId);
                System.out.println("[WS] CONNECT bound userId=" + userId + " to sessionId=" + sessionId);
            }
        }
        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            String sessionId = accessor.getSessionId();
            sessionRegistry.unbind(sessionId);
            System.out.println("[WS] DISCONNECT cleared sessionId=" + sessionId);
        }
        return message;
    }
}

/**
 * Blocks SUBSCRIBE frames that target /topic/room/{roomId} unless the
 * authenticated user is a member of that room. Also blocks sending to
 * /app/send-message/{roomId} or /app/message-status/{roomId} from non-members.
 *
 * User identity is sourced from SessionRegistry (keyed by STOMP sessionId
 * populated by UserBindingInterceptor on CONNECT) - Spring's accessor.getUser()
 * is unreliable across frames in the same session.
 */
@Component
class MembershipInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/room/([^/]+)(/.*)?$");
    private static final Pattern ROOM_APP   = Pattern.compile("^/app/(send-message|message-status)/([^/]+)$");

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        if (command == StompCommand.SUBSCRIBE && destination != null) {
            Matcher m = ROOM_TOPIC.matcher(destination);
            if (m.matches()) {
                String roomId = m.group(1);
                String userId = sessionRegistry.lookup(sessionId);
                if (userId == null) {
                    throw new MembershipDeniedException("Not authenticated for session " + sessionId);
                }
                if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
                    throw new MembershipDeniedException("User " + userId + " not a member of room " + roomId);
                }
            }
        }

        if (command == StompCommand.SEND && destination != null) {
            Matcher m = ROOM_APP.matcher(destination);
            if (m.matches()) {
                String roomId = m.group(2);
                String userId = sessionRegistry.lookup(sessionId);
                if (userId == null) {
                    throw new MembershipDeniedException("Not authenticated for session " + sessionId);
                }
                if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
                    throw new MembershipDeniedException("User " + userId + " not a member of room " + roomId);
                }
            }
        }
        return message;
    }
}

/**
 * Thrown by MembershipInterceptor when a STOMP frame targets a room the
 * principal is not allowed to access. Spring's ChannelInterceptor surfaces
 * any RuntimeException as an ERROR frame, so we don't need Spring Security
 * on the classpath.
 */
class MembershipDeniedException extends RuntimeException {
    public MembershipDeniedException(String message) {
        super(message);
    }
}
