package com.followerservice.repo;

import com.followerservice.entity.Follower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowerRepo extends JpaRepository<Follower,String> {

    @Query("SELECT f.followerId FROM Follower f WHERE f.followingId = :followingId")
    List<String> findFollowerByFollowingId(@Param("followingId") String followingId);

}
