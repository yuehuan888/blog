package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private Long replyTo;
    private String content;
    private Integer likeCount;
    private String status;
    private LocalDateTime createdAt;
    private List<CommentDTO> replies;
    private int replyCount;
    private boolean liked;
}
