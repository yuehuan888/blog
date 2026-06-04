package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String category;

    private String status;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer readCount;

    private Integer commentCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
}
