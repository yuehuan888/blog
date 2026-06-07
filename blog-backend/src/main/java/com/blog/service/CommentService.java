package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.CommentDTO;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;

public interface CommentService {

    Comment create(Long articleId, Long parentId, Long replyTo, String content);

    IPage<CommentDTO> getTopLevel(Long articleId, int page, int size, String sort);

    IPage<CommentDTO> getReplies(Long parentId, int page, int size);

    ToggleResult toggleLike(Long commentId);

    void delete(Long commentId);

    void hide(Long commentId);
}
