package com.postservice.controller;

import com.postservice.dto.CommentMessage;
import com.postservice.dto.PostDto;
import com.postservice.dto.PostResponseDTO;
import com.postservice.dto.Response;
import com.postservice.entity.Post;
import com.postservice.entity.Visibility;
import com.postservice.service.ImageUploadService;
import com.postservice.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("/me")
    public ResponseEntity<String> getUserInfo(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok("User ID from Gateway: " + userId);
    }


    @PostMapping
    public ResponseEntity<Response> createPost(
                @RequestHeader("X-User-Id") String userId,
            @RequestBody PostDto postDto) {
        try {
            Post newPost = new Post();
            newPost.setUserId(userId);
            newPost.setContent(postDto.getContent());
            newPost.setVisibility(postDto.getVisibility());
            newPost = postService.savePost(newPost, postDto.getMediaIds());

            return new ResponseEntity<>(new Response(true, "Created a post", newPost), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Could not create a post", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get all posts of a specific user by userId
    @GetMapping("/user")
    public ResponseEntity<Response> getAllPostsByUser(
            @RequestParam(value = "user", required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String xuserId) {
        try {
            String id=null;
            if(userId==null)
                id=xuserId;
            else
                id=userId;
            System.out.println(id);
            List<com.postservice.dto.PostResponseDTO> posts = postService.getPostsOfUser(id);

            return new ResponseEntity<>(new Response(true, "Posts fetched successfully", posts),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Could not fetch posts", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/{postId}")
    public ResponseEntity<Response> getPostById(@PathVariable String postId) {
        try {
            com.postservice.dto.PostResponseDTO post = postService.getPost(postId);
            if (post == null) {
                return new ResponseEntity<>(new Response(false, "Post not found", null), HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(new Response(true, "Post fetched successfully", post), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Could not fetch post", null), HttpStatus.INTERNAL_SERVER_ERROR);
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

    @GetMapping
    public ResponseEntity<Response> getAllPosts(@RequestHeader("X-USER-ID") String userId){
        return new ResponseEntity<>(new Response(true,"All posts",postService.getPosts(userId)),HttpStatus.OK);
    }

    @PutMapping("/comment-count")
    public ResponseEntity<Response> updateCommentCount(@RequestBody CommentMessage m){
        return  new ResponseEntity<>(new Response(true, "Updates Comment Count", postService.updateCommentCount(m)), HttpStatus.OK);
    }

    @GetMapping("/recommendation-candidates")
    public Response getRecommendationCandidates(
            @RequestParam(defaultValue = "100") int limit,
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        List<com.postservice.dto.PostResponseDTO> posts =
                postService.getRecommendationCandidatesDTO(userId, limit);

        return new Response(
                true,
                "Recommendation candidates fetched successfully",
                posts
        );
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Response> deletePost(@PathVariable String postId){
        try {
            String post = postService.deletePost(postId);
            return new ResponseEntity<>(new Response(true,post,null ), HttpStatus.OK);
        }
        catch (InvalidTargetObjectTypeException e){
            return new ResponseEntity<>(new Response(false, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response(false, "Something went wrong", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
