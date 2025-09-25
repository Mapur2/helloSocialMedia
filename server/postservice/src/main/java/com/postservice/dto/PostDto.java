package com.postservice.dto;

import com.postservice.entity.Visibility;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class PostDto {
    private String content;
    private String mediaUrl;
    private Visibility visibility;
}
