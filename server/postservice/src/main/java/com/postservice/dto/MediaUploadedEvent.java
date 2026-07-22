package com.postservice.dto;

public record MediaUploadedEvent(
        String mediaId,
        String storageKey,
        String mediaType
) {}