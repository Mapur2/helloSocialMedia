package com.postInteractionService.repository;

import com.postInteractionService.dto.CommentUsernameDTO;
import com.postInteractionService.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comment,String> {
    List<Comment> findAllByPostId(String postId);
}
