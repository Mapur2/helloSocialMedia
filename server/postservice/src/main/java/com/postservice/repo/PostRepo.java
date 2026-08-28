package com.postservice.repo;

import com.postservice.entity.Post;
import com.postservice.entity.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepo extends JpaRepository<Post, String> {
    List<Post> findPostByUserId(String userId);

    Post findPostById(String id);

    List<Post> findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
            Visibility visibility,
            Pageable pageable
    );

}
