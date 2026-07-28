package com.followerservice.repo;

import com.followerservice.entity.Follower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepo extends JpaRepository<Follower,String> {

    @Query("SELECT f.followerId FROM Follower f WHERE f.followingId = :followingId")
    List<String> findFollowerByFollowingId(@Param("followingId") String followingId);

    @Query("SELECT f.followingId FROM Follower f WHERE f.followerId = :followerId")
    List<String> findFollowingByFollowerId(@Param("followerId") String followerId);

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    Optional<Follower> findByFollowerIdAndFollowingId(String followerId, String followingId);
}
