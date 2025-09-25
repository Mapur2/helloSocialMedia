package com.postInteractionService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private String userId;
    @Valid
    @NotBlank
    @NotEmpty
    @NotNull
    private String commentorId;
    @Valid
    @NotBlank
    @NotEmpty
    @NotNull
    private String postId;
    @Valid
    @NotBlank
    @NotEmpty
    @NotNull
    private String text;
}
