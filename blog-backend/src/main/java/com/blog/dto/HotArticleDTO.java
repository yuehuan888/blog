package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotArticleDTO {
    private Long articleId;
    private String title;
    private String type;
    private int readCount;

    public HotArticleDTO(Long articleId, String title, int readCount) {
        this.articleId = articleId;
        this.title = title;
        this.readCount = readCount;
    }
}
