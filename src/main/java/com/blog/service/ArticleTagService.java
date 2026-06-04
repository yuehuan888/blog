package com.blog.service;

import com.blog.entity.Tag;

import java.util.List;

public interface ArticleTagService {

    void setTags(Long articleId, List<Long> tagIds);

    List<Tag> getByArticleId(Long articleId);

    void deleteByTagId(Long tagId);
}
