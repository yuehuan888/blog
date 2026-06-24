package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_fingerprint")
public class VideoFingerprint {

    @TableId(type = IdType.INPUT)
    private String sha256;

    private String objectKey;

    private Long articleId;

    private LocalDateTime createdAt;
}
