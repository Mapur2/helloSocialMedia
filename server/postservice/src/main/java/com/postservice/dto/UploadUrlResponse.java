package com.postservice.dto;

import lombok.Data;

public record UploadUrlResponse(
        String mediaId,
        String uploadUrl
) {}