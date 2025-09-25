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
        if(followerDTO.getFollowerId()==null){
            return new ResponseEntity<>(new Response<>(false,"followerId is needed",null), HttpStatus.BAD_REQUEST);
        }
        Follower response = followerService.addFollower(followerDTO.getFollowerId(),userId);

        return new ResponseEntity<>(new Response<>(true,"Added a new follower",response),HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<Response<UserFollowerList>> getFollowers(@RequestHeader("X-USER-ID") String userId){
        return new ResponseEntity<>(new Response<>(true,"All followers",followerService.getFollowers(userId)),HttpStatus.OK);
    }
}
