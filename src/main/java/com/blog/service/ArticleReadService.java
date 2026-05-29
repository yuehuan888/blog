package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.HotArticleDTO;

public interface ArticleReadService {

    void recordRead(Long articleId, Long userId, String ip);

    IPage<HotArticleDTO> getHotArticles(int days, int page, int size);
}
