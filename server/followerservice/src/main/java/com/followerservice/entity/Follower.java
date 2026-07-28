package com.followerservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "followers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"followerId", "followingId"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Follower {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String followerId;     // the user who is following

    @Column(nullable = false)
    private String followingId;    // the user being followed

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
