package com.postservice.repo;

import com.postservice.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

public interface MediaRepository extends JpaRepository<MediaEntity, String> {
}
