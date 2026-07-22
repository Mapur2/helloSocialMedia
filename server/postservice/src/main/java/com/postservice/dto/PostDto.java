package com.postservice.dto;

import com.postservice.entity.Visibility;
import lombok.Data;

@Data
public class PostDto {
    private String content;
    private String mediaId;
    private Visibility visibility;
}
