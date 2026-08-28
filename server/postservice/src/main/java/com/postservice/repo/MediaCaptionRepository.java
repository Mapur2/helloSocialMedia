package com.postservice.repo;

import com.postservice.entity.MediaCaption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaCaptionRepository extends JpaRepository<MediaCaption, String> {
    Optional<MediaCaption> findByMediaId(String mediaId);
}
