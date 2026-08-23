package com.postInteractionService.repository;

import com.postInteractionService.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comment, String> {
    List<Comment> findAllByPostId(String postId);

    @Query("SELECT DISTINCT c.postId FROM Comment c WHERE c.commentorId = :userId OR c.userId = :userId")
    List<String> findDistinctPostIdsByUserId(@Param("userId") String userId);
}
