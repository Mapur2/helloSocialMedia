package com.followerservice.service;

import com.followerservice.dto.UserFollowerList;
import com.followerservice.dto.UserFollowerResponse;
import com.followerservice.dto.UserIds;
import com.followerservice.entity.Follower;
import com.followerservice.repo.FollowerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class FollowerService {

    @Value("${services.userserivce}")
    String userserviceUrl;
    @Autowired
    FollowerRepo followerRepo;

    @Autowired
    private RestTemplate restTemplate;

    public Follower addFollower(String followUserId, String userId) {
        // Prevent self-follow
        if (followUserId.equals(userId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        // Check if already following — if so, it's a duplicate
        if (followerRepo.existsByFollowerIdAndFollowingId(userId, followUserId)) {
            throw new IllegalStateException("Already following this user");
        }

        Follower newFollower = new Follower();
        newFollower.setFollowingId(followUserId);
        newFollower.setFollowerId(userId);
        followerRepo.save(newFollower);
        return newFollower;
    }

    public void removeFollower(String followUserId, String userId) {
        Optional<Follower> existing = followerRepo.findByFollowerIdAndFollowingId(userId, followUserId);
        if (existing.isEmpty()) {
            throw new IllegalStateException("You are not following this user");
        }
        followerRepo.delete(existing.get());
    }

    public boolean isFollowing(String followerId, String followingId) {
        return followerRepo.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public UserFollowerList getFollowers(String userId) {
        List<String> followers = followerRepo.findFollowerByFollowingId(userId);

        // Short-circuit: no followers means no need to call userservice
        if (followers.isEmpty()) {
            return new UserFollowerList(Collections.emptyList());
        }

        UserIds userIds = new UserIds(followers);
        UserFollowerList response = restTemplate.postForObject(
                userserviceUrl,
                userIds,
                UserFollowerList.class
        );

        return response;
    }

    public UserFollowerList getFollowing(String userId) {
        List<String> following = followerRepo.findFollowingByFollowerId(userId);

        if (following.isEmpty()) {
            return new UserFollowerList(Collections.emptyList());
        }

        UserIds userIds = new UserIds(following);
        UserFollowerList response = restTemplate.postForObject(
                userserviceUrl,
                userIds,
                UserFollowerList.class
        );

        return response;
    }
}
