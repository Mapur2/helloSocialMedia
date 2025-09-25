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

import java.util.List;

@Service
public class FollowerService {

    @Value("${services.userserivce}")
    String userserviceUrl;
    @Autowired
    FollowerRepo followerRepo;

    @Autowired
    private RestTemplate restTemplate;

    public Follower addFollower(String followerId,String followingId){
        Follower newFollower = new Follower();
        newFollower.setFollowingId(followingId);
        newFollower.setFollowerId(followerId);
        followerRepo.save(newFollower);
        return  newFollower;
    }

    public UserFollowerList getFollowers(String userId) {
        List<String> followers = followerRepo.findFollowerByFollowingId(userId);
        UserIds userIds = new UserIds(followers);
        UserFollowerList response = restTemplate.postForObject(
                userserviceUrl,
                userIds,
                UserFollowerList.class
        );

        return response;
    }
}
