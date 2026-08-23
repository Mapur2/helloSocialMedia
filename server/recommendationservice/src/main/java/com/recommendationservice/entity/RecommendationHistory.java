package com.recommendationservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_history")
@Data
public class RecommendationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationItemType itemType;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}