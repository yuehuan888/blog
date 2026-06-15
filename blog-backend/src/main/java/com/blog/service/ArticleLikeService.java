package com.blog.service;

import com.blog.dto.ToggleResult;

public interface ArticleLikeService {

    ToggleResult toggle(Long articleId, Long userId);

    boolean isLiked(Long articleId, Long userId);
}
