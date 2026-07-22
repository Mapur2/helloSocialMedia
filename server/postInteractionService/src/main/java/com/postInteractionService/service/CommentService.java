package com.postInteractionService.service;


import com.postInteractionService.dto.*;
import com.postInteractionService.entity.Comment;
import com.postInteractionService.repository.CommentRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.postInteractionService.entity.Like;
import com.postInteractionService.repository.LikeRepo;

@Slf4j
@Service
public class CommentService {
    @Autowired
    private CommentRepo commentRepo;

    @Autowired
    private LikeRepo likeRepo;

    @Value("${services.postservice.url}")
    private String postservice;
    @Value("${services.userservice.url}")
    private String userServiceURL;

    @Autowired
    private RestTemplate restTemplate;

    public Comment createComment(CommentDTO comment){
        Comment newComment = mapDtoToEntity(comment);
        commentRepo.save(newComment);

        try {
            String url = postservice+"/api/posts/comment-count";
            restTemplate.put(url, new CommentMessage(newComment.getPostId(), "INCREASE"), String.class);
        }
        catch (Exception e){
            log.error("Could not publish comment");
        }

        return newComment;
    }

    private Comment mapDtoToEntity(CommentDTO commentDTO){
        Comment newComment = new Comment();
        newComment.setText(commentDTO.getText());
        newComment.setCommentorId(commentDTO.getCommentorId());
        newComment.setPostId(commentDTO.getPostId());
        newComment.setUserId(commentDTO.getUserId());
        return newComment;
    }

    public List<CommentUsernameDTO> getCommentsOfPost(String postId){
        List<Comment> comments =  commentRepo.findAllByPostId(postId);
        List<CommentUsernameDTO> commentUsernameDTOS = new ArrayList<>();
        for(Comment e:comments){
            CommentUsernameDTO commentUsernameDTO = new CommentUsernameDTO();
            commentUsernameDTO.setUserId(e.getUserId());
            commentUsernameDTO.setPostId(e.getPostId());
            commentUsernameDTO.setText(e.getText());
            commentUsernameDTO.setCommentorId(e.getCommentorId());
            commentUsernameDTOS.add(commentUsernameDTO);
        }
        List<String> ids = new ArrayList<>();
        for(CommentUsernameDTO e:commentUsernameDTOS)
            ids.add(e.getCommentorId());
        UserIds userIds = new UserIds();
        userIds.setIds(ids);
        Usernames usernames = restTemplate.postForObject(
                userServiceURL+"/api/users/usernames",
                userIds,
                Usernames.class
        );
        for(CommentUsernameDTO e:commentUsernameDTOS) {
            assert usernames != null;
            e.setCommentorUserName(usernames.getUsers().get(e.getCommentorId()));
        }
        return commentUsernameDTOS;
    }

    public void likePost(String userId, String postId) throws Exception {
        Optional<Like> existingLike = likeRepo.findByUserIdAndPostId(userId, postId);
        String action;
        
        if (existingLike.isPresent()) {
            likeRepo.delete(existingLike.get());
            action = "dislike";
        } else {
            Like like = new Like();
            like.setUserId(userId);
            like.setPostId(postId);
            likeRepo.save(like);
            action = "like";
        }

        try {
            String url = postservice + "/api/posts/react/" + postId + "/" + action;
            restTemplate.put(url, null);
        } catch (Exception e) {
            log.error("Could not update like count in post service");
            throw new Exception("Could not interact with post service", e);
        }
    }

}
