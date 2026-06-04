package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;

public interface ArticleHistoryService {

    IPage<ArticleHistory> getHistory(Long articleId, int page, int size);

    ArticleHistory getDetail(Long historyId);

    Article rollback(Long articleId, Long historyId, Long userId);
}
