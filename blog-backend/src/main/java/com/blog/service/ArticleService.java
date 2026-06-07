package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;

import java.util.List;
import java.util.Map;

public interface ArticleService extends IService<Article> {

    IPage<Article> page(ArticleQueryDTO query);

    Article patch(Long id, Article partial);

    List<Map<String, Object>> categoryStatistics();
}
