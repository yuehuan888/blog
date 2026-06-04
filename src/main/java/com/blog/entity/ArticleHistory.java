package com.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_history")
public class ArticleHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String title;

    private String content;

    private String category;

    private Integer versionNo;

    private String changeType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
