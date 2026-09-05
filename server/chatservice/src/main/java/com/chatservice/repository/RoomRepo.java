package com.chatservice.repository;

import com.chatservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepo extends JpaRepository<Room, String> {

    Room findByRoomId(String roomId);
}
