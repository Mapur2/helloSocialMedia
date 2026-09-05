package com.chatservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chatservice.entity.Message;

@Repository
public interface MessageRepository
        extends JpaRepository<Message, String> {

    Optional<Message> findBySenderIdAndClientMessageId(
            String senderId,
            String clientMessageId
    );

    boolean existsBySenderIdAndClientMessageId(
            String senderId,
            String clientMessageId
    );

    List<Message> findByRoomIdOrderByTimestampAsc(String roomId);

    Page<Message> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);

    List<Message> findByRoomIdAndSenderIdNotAndStatusNot(
            String roomId,
            String senderId,
            com.chatservice.entity.MessageStatus status
    );
}