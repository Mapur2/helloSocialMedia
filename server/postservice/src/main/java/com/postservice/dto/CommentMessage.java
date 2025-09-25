package com.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentMessage {
    private String postId;
    private String type;

    @Override
    public String toString() {
        return "CommentMessage{" +
                "postId='" + postId + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
