package com.chatservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatservice.dto.Response;
import com.chatservice.dto.RoomRequest;
import com.chatservice.entity.Message;
import com.chatservice.entity.Room;
import com.chatservice.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/direct")
    public ResponseEntity<Response<?>> getOrCreateDirectRoom(
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId,
            @RequestBody com.chatservice.dto.DirectRoomRequest request
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            Room r = roomService.getOrCreateDirectRoom(userId, request.getUserId());
            return new ResponseEntity<>(new Response<>(true, "Direct room retrieved successfully", r), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<Response<?>> getMyRooms(
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            List<com.chatservice.dto.RoomResponseDTO> rooms = roomService.getUserRooms(userId);
            return new ResponseEntity<>(new Response<>(true, "User rooms retrieved successfully", rooms), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<Response<?>> createRoom(@RequestBody RoomRequest roomRequest) {
        try {
            Room r = roomService.saveRoom(roomRequest.getRoomId(), roomRequest.getRoomType());
            return new ResponseEntity<>(new Response<>(true, "Room created successfully", r), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Response<?>> joinRoom(@PathVariable String roomId) {
        try {
            Room r = roomService.getRoom(roomId);
            return new ResponseEntity<>(new Response<>(true, "Room successfully retrieved", r), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Response<?>> getMessages(
            @PathVariable String roomId,
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        try {
            List<Message> r = roomService.getMessagesOfRoom(roomId, userId, page, size);
            return new ResponseEntity<>(new Response<>(true, "Messages successfully retrieved", r), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{roomId}/messages/read")
    public ResponseEntity<Response<?>> markMessagesAsRead(
            @PathVariable String roomId,
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            List<Message> updatedMessages = roomService.markRoomMessagesAsRead(roomId, userId);
            // Notify every room member (especially the original senders) so
            // their UI can flip ticks to "read".
            broadcastReadReceipts(roomId, updatedMessages);
            return new ResponseEntity<>(new Response<>(true, "Messages marked as read", updatedMessages), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/group")
    public ResponseEntity<Response<?>> createGroupRoom(
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId,
            @RequestBody com.chatservice.dto.GroupRoomRequest request
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            Room r = roomService.createGroupRoom(userId, request.getName(), request.getMemberIds());
            return new ResponseEntity<>(new Response<>(true, "Group room created successfully", r), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<Response<?>> getRoomMembers(
            @PathVariable String roomId,
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        try {
            List<com.chatservice.entity.RoomMember> members = roomService.getRoomMembers(roomId, userId);
            return new ResponseEntity<>(new Response<>(true, "Room members retrieved successfully", members), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<Response<?>> addMemberToGroup(
            @PathVariable String roomId,
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId,
            @RequestBody com.chatservice.dto.DirectRoomRequest request
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            com.chatservice.entity.RoomMember newMember = roomService.addMemberToGroup(roomId, userId, request.getUserId());
            return new ResponseEntity<>(new Response<>(true, "Member added successfully", newMember), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{roomId}/members/{memberId}")
    public ResponseEntity<Response<?>> removeMemberFromGroup(
            @PathVariable String roomId,
            @PathVariable String memberId,
            @RequestHeader(value = "X-USER-ID", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Id", required = false) String altUserId
    ) {
        String userId = currentUserId != null ? currentUserId : altUserId;
        if (userId == null || userId.isBlank()) {
            return new ResponseEntity<>(new Response<>(false, "User authentication required (X-USER-ID header missing)", null), HttpStatus.UNAUTHORIZED);
        }
        try {
            roomService.removeMemberFromGroup(roomId, userId, memberId);
            return new ResponseEntity<>(new Response<>(true, "Member removed successfully", null), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Send a STOMP status update for every message that was just flipped to
     * READ. Membership is gated by the SUBSCRIBE interceptor on the topic,
     * so we can broadcast plainly.
     */
    private void broadcastReadReceipts(String roomId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (Message msg : messages) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/status",
                    new Response<>(true, "Status updated", msg)
            );
        }
    }
}
