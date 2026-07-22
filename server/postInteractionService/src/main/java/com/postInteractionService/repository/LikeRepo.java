package com.postInteractionService.repository;

import com.postInteractionService.entity.Comment;
import com.postInteractionService.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepo extends  JpaRepository<Like, String>{
    Optional<Like> findByUserIdAndPostId(String userId, String postId);
}
