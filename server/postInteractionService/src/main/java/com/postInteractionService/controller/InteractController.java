package com.postInteractionService.controller;

import com.postInteractionService.dto.CommentDTO;
import com.postInteractionService.dto.CommentUsernameDTO;
import com.postInteractionService.dto.Response;
import com.postInteractionService.entity.Comment;
import com.postInteractionService.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return ResponseEntity.badRequest().body(new Response<>(false,  "Something went wrong",""));
        }
    }

}
