package com.chatservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatservice.dto.SendMessageRequest;
import com.chatservice.entity.Message;
import com.chatservice.entity.Room;
import com.chatservice.entity.RoomMember;
import com.chatservice.entity.RoomType;
import com.chatservice.repository.MessageRepository;
import com.chatservice.repository.RoomMemberRepository;
import com.chatservice.repository.RoomRepo;

@Service
public class RoomService {

    @Autowired
    private RoomRepo roomRepo;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageService messageService;

    @Transactional
    public Room getOrCreateDirectRoom(String currentUserId, String targetUserId) throws Exception {
        if (currentUserId == null || targetUserId == null || currentUserId.isBlank() || targetUserId.isBlank()) {
            throw new IllegalArgumentException("User IDs cannot be null or empty");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot create direct chat with oneself");
        }

        // 1. Lookup with both orderings (a->b and b->a)
        Optional<String> existingRoomId = roomMemberRepository.findDirectRoomIdBetweenUsers(currentUserId, targetUserId);
        if (existingRoomId.isPresent()) {
            Room existingRoom = roomRepo.findByRoomId(existingRoomId.get());
            if (existingRoom != null) {
                return existingRoom;
            }
        }

        // 2. Create new Room
        String newRoomId = UUID.randomUUID().toString();
        Room newRoom = Room.builder()
                .roomId(newRoomId)
                .roomType(RoomType.ONE_TO_ONE)
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        newRoom = roomRepo.save(newRoom);

        // 3. Add both members (in sorted order to make the unique pair
        //    (roomType, lowUser, highUser) deterministic).
        String lowUser = currentUserId.compareTo(targetUserId) < 0 ? currentUserId : targetUserId;
        String highUser = currentUserId.compareTo(targetUserId) < 0 ? targetUserId : currentUserId;

        RoomMember member1 = RoomMember.builder()
                .roomId(newRoomId)
                .userId(lowUser)
                .role("MEMBER")
                .joinedAt(LocalDateTime.now())
                .build();

        RoomMember member2 = RoomMember.builder()
                .roomId(newRoomId)
                .userId(highUser)
                .role("MEMBER")
                .joinedAt(LocalDateTime.now())
                .build();

        try {
            roomMemberRepository.saveAll(List.of(member1, member2));
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Race lost: another concurrent request just inserted members
            // for the same pair. Discard our half-created room and return
            // the winner.
            roomRepo.delete(newRoom);
            Optional<String> winner = roomMemberRepository.findDirectRoomIdBetweenUsers(currentUserId, targetUserId);
            if (winner.isPresent()) {
                Room w = roomRepo.findByRoomId(winner.get());
                if (w != null) return w;
            }
            throw dup;
        }

        return newRoom;
    }

    public List<com.chatservice.dto.RoomResponseDTO> getUserRooms(String userId) {
        List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
        List<String> roomIds = memberships.stream().map(RoomMember::getRoomId).toList();
        return roomIds.stream()
                .map(roomRepo::findByRoomId)
                .filter(java.util.Objects::nonNull)
                .map(room -> {
                    List<RoomMember> allMembers = roomMemberRepository.findByRoomId(room.getRoomId());
                    List<String> memberUserIds = allMembers.stream().map(RoomMember::getUserId).toList();
                    String otherUserId = null;
                    if (room.getRoomType() == RoomType.ONE_TO_ONE) {
                        otherUserId = memberUserIds.stream()
                                .filter(id -> !id.equals(userId))
                                .findFirst()
                                .orElse(null);
                    }

                    return com.chatservice.dto.RoomResponseDTO.builder()
                            .id(room.getId())
                            .roomId(room.getRoomId())
                            .roomType(room.getRoomType())
                            .name(room.getName())
                            .createdBy(room.getCreatedBy())
                            .memberIds(memberUserIds)
                            .otherUserId(otherUserId)
                            .createdAt(room.getCreatedAt())
                            .updatedAt(room.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    public Room saveRoom(String roomId, RoomType roomType) throws Exception {
        if (roomRepo.findByRoomId(roomId) != null)
            throw new Exception("roomId is already taken");
        Room room = Room.builder()
                .roomId(roomId)
                .roomType(roomType)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return roomRepo.save(room);
    }

    public Room getRoom(String roomId) throws Exception {
        Room room = roomRepo.findByRoomId(roomId);
        if (room == null)
            throw new Exception("room not found");
        return room;
    }

    public List<Message> getMessagesOfRoom(String roomId, String userId, int page, int size) throws Exception {
        Room room = this.getRoom(roomId);
        if (userId != null && !userId.isBlank() && !roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new SecurityException("User is not a member of room " + roomId);
        }
        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageNumber = Math.max(page, 0);
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
        return messageRepository.findByRoomIdOrderByTimestampDesc(roomId, pageable).getContent();
    }

    @Transactional
    public Message sendMessageToRoom(SendMessageRequest messageRequest, String roomId) throws Exception {
        if (messageRequest == null || messageRequest.getSenderId() == null || messageRequest.getSenderId().isBlank()) {
            throw new IllegalArgumentException("Sender ID is required");
        }

        // 1. Verify room exists
        Room room = this.getRoom(roomId);

        // 2. Verify sender is a member of this room
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, messageRequest.getSenderId())) {
            throw new SecurityException("User " + messageRequest.getSenderId() + " is not a member of room " + roomId);
        }

        // 3. Idempotency check
        if (messageRequest.getClientMessageId() != null && !messageRequest.getClientMessageId().isBlank()) {
            Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(
                    messageRequest.getSenderId(),
                    messageRequest.getClientMessageId()
            );
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // 4. Save and return new message
        return messageService.saveMessage(messageRequest, roomId);
    }

    @Transactional
    public List<Message> markRoomMessagesAsRead(String roomId, String currentUserId) throws Exception {
        Room room = this.getRoom(roomId);
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, currentUserId)) {
            throw new SecurityException("User is not a member of room " + roomId);
        }
        return messageService.markRoomMessagesAsRead(roomId, currentUserId);
    }

    @Transactional
    public Room createGroupRoom(String creatorId, String name, List<String> memberIds) {
        if (creatorId == null || creatorId.isBlank()) {
            throw new IllegalArgumentException("Creator ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }

        String newRoomId = UUID.randomUUID().toString();
        Room groupRoom = Room.builder()
                .roomId(newRoomId)
                .roomType(RoomType.GROUP)
                .name(name.trim())
                .createdBy(creatorId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        groupRoom = roomRepo.save(groupRoom);

        // Add creator as ADMIN
        RoomMember creatorMember = RoomMember.builder()
                .roomId(newRoomId)
                .userId(creatorId)
                .role("ADMIN")
                .joinedAt(LocalDateTime.now())
                .build();
        roomMemberRepository.save(creatorMember);

        // Add initial members
        if (memberIds != null) {
            for (String memberId : memberIds) {
                if (memberId != null && !memberId.isBlank() && !memberId.equals(creatorId)) {
                    if (!roomMemberRepository.existsByRoomIdAndUserId(newRoomId, memberId)) {
                        RoomMember member = RoomMember.builder()
                                .roomId(newRoomId)
                                .userId(memberId)
                                .role("MEMBER")
                                .joinedAt(LocalDateTime.now())
                                .build();
                        roomMemberRepository.save(member);
                    }
                }
            }
        }

        return groupRoom;
    }

    @Transactional
    public RoomMember addMemberToGroup(String roomId, String requesterId, String newMemberId) throws Exception {
        Room room = this.getRoom(roomId);
        if (room.getRoomType() != RoomType.GROUP) {
            throw new IllegalArgumentException("Cannot add members to a ONE_TO_ONE room");
        }

        RoomMember requester = roomMemberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new SecurityException("You are not a member of this group"));

        if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
            throw new SecurityException("Only group admins can add new members");
        }

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, newMemberId)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        RoomMember newMember = RoomMember.builder()
                .roomId(roomId)
                .userId(newMemberId)
                .role("MEMBER")
                .joinedAt(LocalDateTime.now())
                .build();

        return roomMemberRepository.save(newMember);
    }

    @Transactional
    public void removeMemberFromGroup(String roomId, String requesterId, String memberToRemoveId) throws Exception {
        Room room = this.getRoom(roomId);
        if (room.getRoomType() != RoomType.GROUP) {
            throw new IllegalArgumentException("Cannot remove members from a ONE_TO_ONE room");
        }

        RoomMember requester = roomMemberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new SecurityException("You are not a member of this group"));

        // User can remove themselves (leave group) or admin can remove any member
        boolean isSelf = requesterId.equals(memberToRemoveId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requester.getRole());

        if (!isSelf && !isAdmin) {
            throw new SecurityException("Only admins can remove other members");
        }

        roomMemberRepository.deleteByRoomIdAndUserId(roomId, memberToRemoveId);
    }

    public List<RoomMember> getRoomMembers(String roomId, String requesterId) throws Exception {
        Room room = this.getRoom(roomId);
        if (requesterId != null && !requesterId.isBlank() && !roomMemberRepository.existsByRoomIdAndUserId(roomId, requesterId)) {
            throw new SecurityException("You are not a member of room " + roomId);
        }
        return roomMemberRepository.findByRoomId(roomId);
    }
}
