package com.postservice.controller;

import com.postservice.dto.Response;
import com.postservice.entity.Post;
import com.postservice.entity.Visibility;
import com.postservice.service.ImageUploadService;
import com.postservice.service.PostService;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private ImageUploadService imageUploadService;
    @Autowired
    private PostService postService;

    @GetMapping("/me")
    public ResponseEntity<String> getUserInfo(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok("User ID from Gateway: " + userId);
    }


    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Response> createPost(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("content") String content,
            @RequestParam("visibility") Visibility visibility,
            @RequestParam("mediaFile") MultipartFile mediaFile) {
        try {
            String url = imageUploadService.uploadFile(mediaFile).get("secure_url").toString();
            Post newPost = new Post();
            newPost.setUserId(userId);
            newPost.setContent(content);
            newPost.setVisibility(visibility);
            newPost.setMediaUrl(url);
            newPost = postService.savePost(newPost);

            return new ResponseEntity<>(new Response(true, "Created a post", newPost), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Could not create a post", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Get all posts of a specific user by userId
    @GetMapping("/user")
    public ResponseEntity<Response> getAllPostsByUser(@PathParam("userId") String userId,@RequestHeader("X-User-Id") String xuserId) {
        try {
            String id=null;
            if(userId==null)
                id=xuserId;
            else
                id=userId;
            System.out.println(id);
            List<Post> posts = postService.getPostsOfUser(id);

            return new ResponseEntity<>(new Response(true, "Posts fetched successfully", posts),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Could not fetch posts", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping("/react/{postId}/{action}")
    public ResponseEntity<Response> likeDislikePost(@PathVariable String postId,@PathVariable String action){
        try {
            int likes = postService.updateLikeCount(postId,action);
            return new ResponseEntity<>(new Response(true,"Reacted the post","action: "+action),HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(new Response(false,"Failed",e.getMessage()),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
