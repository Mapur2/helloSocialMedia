package com.userservice.repository;

import com.userservice.entity.ProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilePictureRepo extends JpaRepository<ProfilePicture, String> {
    Optional<ProfilePicture> findByUserId(String userId);
}
