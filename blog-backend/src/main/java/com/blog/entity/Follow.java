package com.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follow")
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long followerId;

    private Long followingId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
