package com.postInteractionService.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Table(name = "comments")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        @Column(nullable = false)
        private String userId;

        @Column(nullable = false)
        private String commentorId;

        @Column(nullable = false)
        private String postId;

        @Column(columnDefinition = "TEXT")
        private String text;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        @Column(nullable = false)
        private LocalDateTime updatedAt = LocalDateTime.now();

        @PreUpdate
        public void setLastUpdate() {
            this.updatedAt = LocalDateTime.now();
        }
}
