package com.chatservice.repository;

import com.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message,String> {
    List<Message> findAllBySenderIdAndReceiverId(String senderId, String receiverId);
    @Query("SELECT m FROM Message m WHERE (m.senderId = :senderId AND m.receiverId = :receiverId) " +
            "OR (m.senderId = :receiverId AND m.receiverId = :senderId) ORDER BY m.timestamp ASC")
    List<Message> findConversation(@Param("senderId") String senderId,
                                   @Param("receiverId") String receiverId);
}
