package com.postInteractionService.controller;

import com.postInteractionService.dto.CommentDTO;
import com.postInteractionService.dto.CommentUsernameDTO;
import com.postInteractionService.dto.LikeDTO;
import com.postInteractionService.dto.Response;
import com.postInteractionService.entity.Comment;
import com.postInteractionService.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/post/interact")
public class InteractController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/comments")
    public ResponseEntity<Response<Comment>> newComment(@RequestBody CommentDTO comment, @RequestHeader("X-USER-ID") String userId){
        try{
            comment.setUserId(userId);
            Comment newComment = commentService.createComment(comment);
            return new ResponseEntity<>(new Response<>(true,"Added a new comment",newComment),HttpStatus.CREATED);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response<>(false,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/comments/{postId}")
    public ResponseEntity<Response<List<CommentUsernameDTO>>> allComments(@PathVariable String postId){
        try{
            List<CommentUsernameDTO> comments = commentService.getCommentsOfPost(postId);
            return new ResponseEntity<>(new Response<>(true,"All comments",comments),HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response<>(false,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/like/{postId}")
    public ResponseEntity<Response<String>> likePost(@RequestHeader("X-USER-ID") String userId, @PathVariable String postId){
        try{
            commentService.likePost(userId, postId);
            return new ResponseEntity<>(new Response<>(true, "Liked successfully", "" ), HttpStatus.OK);
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(new Response<>(false,  "Something went wrong",null));
        }
    }

    @GetMapping("/like/{postId}")
    public ResponseEntity<Response<List<LikeDTO>>> allLikes(@PathVariable String postId){
        try{
            List<LikeDTO> comments = commentService.getLikesOfPost(postId);
            return new ResponseEntity<>(new Response<>(true,"All likes",comments),HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response<>(false,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/liked-by/{userId}/{postId}")
    public ResponseEntity<Response<Boolean>> isLikedByUser(@PathVariable String postId,  @PathVariable String userId){
        try {
            return new ResponseEntity<>(new Response<>(true, "Here is your result", commentService.isLikedByUser(userId,postId)), HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response<>(false,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // which of these posts has this user liked?
    @PostMapping("/liked-by/{userId}/batch")
    public ResponseEntity<Response<Map<String, Boolean>>> isPostsLikedByUser(
            @PathVariable String userId,
            @RequestBody List<String> postIds) {
        try {
            return new ResponseEntity<>(
                    new Response<>(true, "Here is your result",
                            commentService.isPostsLikedByUser(userId, postIds)),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new Response<>(false, e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
