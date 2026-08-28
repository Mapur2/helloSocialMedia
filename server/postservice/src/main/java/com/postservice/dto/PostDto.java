package com.postservice.dto;

import com.postservice.entity.Visibility;
import lombok.Data;
import java.util.List;

@Data
public class PostDto {
    private String content;
    private List<String> mediaIds;
    private Visibility visibility;
}
