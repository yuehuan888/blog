package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_image")
public class ArticleImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String url;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
