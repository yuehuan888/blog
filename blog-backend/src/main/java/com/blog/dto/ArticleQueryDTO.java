package com.blog.dto;

import lombok.Data;

@Data
public class ArticleQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private String category;
    private String status;
    private String keyword;
    private Long tagId;
    private Long authorId;
}
