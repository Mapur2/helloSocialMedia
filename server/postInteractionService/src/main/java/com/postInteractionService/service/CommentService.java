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

import java.util.*;
import java.util.stream.Collectors;

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

    public Comment createComment(CommentDTO comment) {
        Comment newComment = mapDtoToEntity(comment);
        commentRepo.save(newComment);

        try {
            String url = postservice + "/api/posts/comment-count";
            restTemplate.put(url, new CommentMessage(newComment.getPostId(), "INCREASE"), String.class);
        } catch (Exception e) {
            log.error("Could not publish comment");
        }

        return newComment;
    }

    private Comment mapDtoToEntity(CommentDTO commentDTO) {
        Comment newComment = new Comment();
        newComment.setText(commentDTO.getText());
        newComment.setCommentorId(commentDTO.getCommentorId());
        newComment.setPostId(commentDTO.getPostId());
        newComment.setUserId(commentDTO.getUserId());
        return newComment;
    }

    public List<CommentUsernameDTO> getCommentsOfPost(String postId) {
        List<Comment> comments = commentRepo.findAllByPostId(postId);
        List<CommentUsernameDTO> commentUsernameDTOS = new ArrayList<>();
        for (Comment e : comments) {
            CommentUsernameDTO commentUsernameDTO = new CommentUsernameDTO();
            commentUsernameDTO.setUserId(e.getUserId());
            commentUsernameDTO.setPostId(e.getPostId());
            commentUsernameDTO.setText(e.getText());
            commentUsernameDTO.setCommentorId(e.getCommentorId());
            commentUsernameDTOS.add(commentUsernameDTO);
        }
        List<String> ids = new ArrayList<>();
        for (CommentUsernameDTO e : commentUsernameDTOS)
            ids.add(e.getCommentorId());
        UserIds userIds = new UserIds();
        userIds.setIds(ids);
        Usernames usernames = restTemplate.postForObject(
                userServiceURL + "/api/users/usernames",
                userIds,
                Usernames.class
        );
        for (CommentUsernameDTO e : commentUsernameDTOS) {
            assert usernames != null;
            e.setCommentorUserName(usernames.getUsers().get(e.getCommentorId()));
        }
        return commentUsernameDTOS;
    }

    public List<LikeDTO> getLikesOfPost(String postId) {
        List<Like> likes = likeRepo.findAllByPostId(postId);

        if (likes.isEmpty()) return List.of();

        // map to DTO — username not set yet
        List<LikeDTO> likeDTOs = new ArrayList<>();
        for (Like like : likes) {
            LikeDTO dto = new LikeDTO();
            dto.setUserId(like.getUserId());
            dto.setPostId(like.getPostId());
            likeDTOs.add(dto);
        }

        // collect all userIds for batch fetch
        List<String> ids = likeDTOs.stream()
                .map(LikeDTO::getUserId)
                .toList();

        // one call to userservice for all usernames
        UserIds userIds = new UserIds();
        userIds.setIds(ids);

        try {
            Usernames usernames = restTemplate.postForObject(
                    userServiceURL + "/api/users/usernames",
                    userIds,
                    Usernames.class
            );

            if (usernames != null && usernames.getUsers() != null) {
                for (LikeDTO dto : likeDTOs) {
                    dto.setUsername(
                            usernames.getUsers().getOrDefault(dto.getUserId(), "Unknown")
                    );
                }
            }
        } catch (Exception e) {
            log.error("Could not fetch usernames from userservice: {}", e.getMessage());
            // don't fail the whole request — just return without usernames
            likeDTOs.forEach(dto -> dto.setUsername("Unknown"));
        }

        return likeDTOs;
    }

    public void likePost(String userId, String postId) throws Exception {
        Optional<Like> existingLike = likeRepo.findByUserIdAndPostId(userId, postId);
        String action;

        if (existingLike.isPresent()) {
            System.out.println("disliking");
            likeRepo.delete(existingLike.get());
            action = "dislike";
        } else {
            Like like = new Like();
            like.setUserId(userId);
            like.setPostId(postId);
            log.info("Attempting to save like: userId={}, postId={}", userId, postId);
            try {
                likeRepo.save(like);
                log.info("Like saved successfully");
            } catch (Exception e) {
                log.error("Failed to save like: {}", e.getMessage(), e);
                throw e;  // rethrow so you see the real error
            }
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

    public boolean isLikedByUser(String userId,String postId){
        Optional<Like> existingLike = likeRepo.findByUserIdAndPostId(userId, postId);
        return existingLike.isPresent();
    }

    public Map<String, Boolean> isPostsLikedByUser(String userId, List<String> postIds) {
        List<Like> likes = likeRepo.findByUserIdAndPostIdIn(userId, postIds);

        Set<String> likedPostIds = likes.stream()
                .map(Like::getPostId)
                .collect(Collectors.toSet());

        Map<String, Boolean> result = new HashMap<>();
        for (String postId : postIds) {
            result.put(postId, likedPostIds.contains(postId));
        }
        return result;
    }

}
