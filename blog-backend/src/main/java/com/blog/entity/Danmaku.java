package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("danmaku")
public class Danmaku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    private String content;

    private Double timestampSec;

    private String color;

    private String mode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String MODE_SCROLL = "scroll";
    public static final String MODE_TOP = "top";
    public static final String MODE_BOTTOM = "bottom";
}
