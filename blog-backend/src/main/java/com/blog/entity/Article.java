package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    private Long authorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String coverImage;

    @TableField(exist = false)
    private List<String> images;

    @TableField(exist = false)
    private Integer imageCount;

    // ====== Video fields ======
    private String type;

    private String videoUrl;

    private String thumbnailUrl;

    private Integer duration;

    private String aiSummary;

    private String transcodeStatus;

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
    public static final String TYPE_ARTICLE = "article";
    public static final String TYPE_VIDEO = "video";
    public static final String TRANSCODE_PENDING = "pending";
    public static final String TRANSCODE_PROCESSING = "processing";
    public static final String TRANSCODE_DONE = "done";
    public static final String TRANSCODE_FAILED = "failed";
}
