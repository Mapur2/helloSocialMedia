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

@Slf4j
@Service
public class CommentService {

    @Value("${rabbitmq.exchanges.comment}")
    private String commentExchange;

    @Value("${rabbitmq.routing.comment}")
    private String commentRoutingKey;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CommentRepo commentRepo;

    @Value("${services.userservice.url}")
    private String userServiceURL;
    @Autowired
    private RestTemplate restTemplate;

    public Comment createComment(CommentDTO comment){
        Comment newComment = mapDtoToEntity(comment);
        commentRepo.save(newComment);

        try {
            rabbitTemplate.convertAndSend(commentExchange,commentRoutingKey,new CommentMessage(comment.getPostId(),"INCREASE"));
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

}
