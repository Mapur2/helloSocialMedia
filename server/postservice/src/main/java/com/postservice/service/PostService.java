package com.postservice.service;

import com.postservice.dto.CommentMessage;
import com.postservice.dto.PostDto;
import com.postservice.dto.PostResponseDTO;
import com.postservice.entity.Post;
import com.postservice.repo.MediaRepository;
import com.postservice.repo.PostRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PostService {
    @Autowired
    PostRepo postRepo;

    @Autowired
    MediaRepository mediaRepository;
    @Autowired
    MediaService mediaService;

    public Post savePost(Post postDto, String mediaId){
        if (mediaId != null && !mediaId.isEmpty()) {
            com.postservice.entity.MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));
            postDto.setMediaId(mediaId);
        }
        postRepo.save(postDto);
        return postDto;
    }

    public List<PostResponseDTO> getPostsOfUser(String userId){
        return postRepo.findPostByUserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public PostResponseDTO getPost(String postId){
        Post e = postRepo.findPostById(postId);
        if (e == null) {
            return null;
        }
        return mapToDTO(e);
    }

    private PostResponseDTO mapToDTO(Post e) {
        Map<String, String> mediaUrls = Collections.emptyMap();
        if (e.getMediaId() != null && !e.getMediaId().isEmpty()) {
            mediaUrls = mediaService.getUrlsByMediaId(e.getMediaId());
            String type = mediaService.getMediaById(e.getMediaId()).getMediaType();
            mediaUrls.put("type",type);
        }

        return new PostResponseDTO(
                e.getId(), 
                e.getContent(), 
                mediaUrls,
                e.getVisibility(), 
                e.getLikeCount(), 
                e.getCommentCount(), 
                e.getShareCount(), 
                e.getCreatedAt(), 
                e.getUpdatedAt(), 
                e.getIsDeleted()
        );
    }

//    @RabbitListener(queues = "comment.queue")
    public String updateCommentCount(CommentMessage message){
        log.info("Recieved activity: "+message);

        Post post = postRepo.findPostById(message.getPostId());
        if (post==null){
            log.error("No such post "+message);
            return "No such post ";
        }

        if(message.getType().equalsIgnoreCase("INCREASE")){
            post.setCommentCount(post.getCommentCount()+1);
            System.out.println("increasing count");
        } else if (message.getType().equalsIgnoreCase("DECREASE")) {
            post.setCommentCount(post.getCommentCount()-1);
            System.out.println("decreasing count");
        }

        log.info("updating count");

        postRepo.save(post);
        return "Comment count: "+post.getCommentCount();
    }

    public int updateLikeCount(String postId, String action) throws Exception {
        Post post = postRepo.findPostById(postId);
        if(action.equalsIgnoreCase("like"))
            post.setLikeCount(post.getLikeCount()+1);
        else if(action.equalsIgnoreCase("dislike"))
            post.setLikeCount(post.getLikeCount()-1);
        else
            throw new Exception("Invalid action");
        postRepo.save(post);
        return post.getLikeCount();
    }

    public List<PostResponseDTO> getPosts(){
        return postRepo.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }
}
