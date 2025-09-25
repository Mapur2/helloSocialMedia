package com.postservice.service;

import com.postservice.dto.CommentMessage;
import com.postservice.dto.PostDto;
import com.postservice.entity.Post;
import com.postservice.repo.PostRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PostService {
    @Autowired
    PostRepo postRepo;

    public Post savePost(Post postDto){
        postRepo.save(postDto);
        return postDto;
    }

    public List<Post> getPostsOfUser(String userId){
        return postRepo.findPostByUserId(userId);
    }
    public Post getPost(String postId){
        return postRepo.findPostById(postId);
    }

    @RabbitListener(queues = "comment.queue")
    private void updateCommentCount(CommentMessage message){
        log.info("Recieved activity: "+message);

        Post post = getPost(message.getPostId());
        if (post==null){
            log.error("No such post "+message);
            return;
        }

        if(message.getType().equals("INCREASE")){
            post.setCommentCount(post.getCommentCount()+1);
            System.out.println("increasing count");
        } else if (message.getType().equals("DECREASE")) {
            post.setCommentCount(post.getCommentCount()-1);
            System.out.println("decreasing count");
        }

        log.info("updating count");

        postRepo.save(post);

    }

    public int updateLikeCount(String postId, String action) throws Exception {
        Post post = postRepo.findPostById(postId);
        if(action.equals("like"))
            post.setLikeCount(post.getLikeCount()+1);
        else if(action.equals("dislike"))
            post.setLikeCount(post.getLikeCount()-1);
        else
            throw new Exception("Invalid action");
        postRepo.save(post);
        return post.getLikeCount();
    }
}
