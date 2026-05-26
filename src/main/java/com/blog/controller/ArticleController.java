package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.ArticleQueryDTO;
import com.blog.dto.Result;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public Result<Article> create(@RequestBody Article article) {
        articleService.save(article);
        return Result.ok(article);
    }

    @GetMapping
    public Result<IPage<Article>> list(ArticleQueryDTO query) {
        return Result.ok(articleService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id) {
        return Result.ok(articleService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Long id, @RequestBody Article article) {
        article.setId(id);
        articleService.updateById(article);
        return Result.ok(articleService.getById(id));
    }

    @PatchMapping("/{id}")
    public Result<Article> patch(@PathVariable Long id, @RequestBody Article article) {
        return Result.ok(articleService.patch(id, article));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/statistics/category")
    public Result<List<Map<String, Object>>> categoryStatistics() {
        return Result.ok(articleService.categoryStatistics());
    }
}
