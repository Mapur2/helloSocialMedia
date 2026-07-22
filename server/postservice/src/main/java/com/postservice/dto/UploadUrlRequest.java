package com.postservice.dto;

public record UploadUrlRequest(
        String filename,
        String contentType,   // e.g. "image/jpeg", "video/mp4"
        String mediaType      // "image" or "video"
) {
    public String extension() {
        return filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1)
                : "";
    }
}