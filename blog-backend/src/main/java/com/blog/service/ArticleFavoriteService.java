package com.blog.service;

import com.blog.dto.ToggleResult;

public interface ArticleFavoriteService {

    ToggleResult toggle(Long articleId, Long userId);

    boolean isFavorited(Long articleId, Long userId);
}
