package com.chatservice.repository;

import com.chatservice.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, String> {

    boolean existsByRoomIdAndUserId(String roomId, String userId);

    Optional<RoomMember> findByRoomIdAndUserId(String roomId, String userId);

    List<RoomMember> findByRoomId(String roomId);

    List<RoomMember> findByUserId(String userId);

    void deleteByRoomIdAndUserId(String roomId, String userId);

    @Query("""
        SELECT rm1.roomId
        FROM RoomMember rm1
        JOIN RoomMember rm2 ON rm1.roomId = rm2.roomId
        JOIN Room r ON r.roomId = rm1.roomId
        WHERE rm1.userId = :userA
          AND rm2.userId = :userB
          AND r.roomType = com.chatservice.entity.RoomType.ONE_TO_ONE
    """)
    Optional<String> findDirectRoomIdBetweenUsers(@Param("userA") String userA, @Param("userB") String userB);
}
