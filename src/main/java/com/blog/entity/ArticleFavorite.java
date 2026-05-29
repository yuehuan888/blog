package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_favorite")
public class ArticleFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
