package com.postservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media")
@Data
@ToString
public class MediaEntity {

    @Id
    private String id;  // ← just @Id, no @GeneratedValue

    private String userId;
    private String mediaType;
    private String status;
    private String storageKey;

    @Column(columnDefinition = "TEXT")
    private String processedKeysJson;

    private Instant createdAt;
    private Instant updatedAt;
}
