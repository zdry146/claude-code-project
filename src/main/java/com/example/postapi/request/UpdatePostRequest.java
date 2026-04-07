package com.example.postapi.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    private String content;

    @Size(max = 50, message = "作者名称长度不能超过50字符")
    private String authorName;

    private String coverImage;

    private Boolean isPublished;
}
