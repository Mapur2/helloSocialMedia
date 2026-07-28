package com.followerservice.controller;


import com.followerservice.dto.FollowerDTO;
import com.followerservice.dto.Response;
import com.followerservice.dto.UserFollowerList;
import com.followerservice.dto.UserFollowerResponse;
import com.followerservice.entity.Follower;
import com.followerservice.service.FollowerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/followers")
public class FollowerController {

    @Autowired
    private FollowerService followerService;


    @PostMapping
    public ResponseEntity<Response<Follower>> addFollower(@RequestBody FollowerDTO followerDTO, @RequestHeader("X-USER-ID") String userId){
        if(followerDTO.getFollowUserId()==null){
            return new ResponseEntity<>(new Response<>(false,"followerId is needed",null), HttpStatus.BAD_REQUEST);
        }
        try {
            Follower response = followerService.addFollower(followerDTO.getFollowUserId(), userId);
            return new ResponseEntity<>(new Response<>(true, "Added a new follower", response), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping
    public ResponseEntity<Response<String>> removeFollower(@RequestBody FollowerDTO followerDTO, @RequestHeader("X-USER-ID") String userId) {
        if (followerDTO.getFollowUserId() == null) {
            return new ResponseEntity<>(new Response<>(false, "followerId is needed", null), HttpStatus.BAD_REQUEST);
        }
        try {
            followerService.removeFollower(followerDTO.getFollowUserId(), userId);
            return new ResponseEntity<>(new Response<>(true, "Unfollowed successfully", null), HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new Response<>(false, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<Response<UserFollowerList>> getFollowers(@RequestHeader(name = "X-USER-ID", required = false) String xuserId, @RequestParam(required = false) String user){
        String id = user;
        if(user==null)
                id = xuserId;
        return new ResponseEntity<>(new Response<>(true,"All followers",followerService.getFollowers(id)),HttpStatus.OK);
    }

    @GetMapping("/following")
    public ResponseEntity<Response<UserFollowerList>> getFollowing(@RequestHeader(name = "X-USER-ID", required = false) String xuserId, @RequestParam(required = false) String user) {
        String id = user;
        if (user == null)
            id = xuserId;
        return new ResponseEntity<>(new Response<>(true, "All following", followerService.getFollowing(id)), HttpStatus.OK);
    }

    @GetMapping("/is-following")
    public ResponseEntity<Response<Boolean>> isFollowing(@RequestHeader("X-USER-ID") String userId, @RequestParam String targetUserId) {
        boolean following = followerService.isFollowing(userId, targetUserId);
        return new ResponseEntity<>(new Response<>(true, "Follow status", following), HttpStatus.OK);
    }
}
