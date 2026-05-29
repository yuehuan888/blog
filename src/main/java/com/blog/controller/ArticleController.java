package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.ArticleQueryDTO;
import com.blog.dto.HotArticleDTO;
import com.blog.dto.Result;
import com.blog.dto.ToggleResult;
import com.blog.entity.Article;
import com.blog.service.ArticleFavoriteService;
import com.blog.service.ArticleLikeService;
import com.blog.service.ArticleReadService;
import com.blog.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleLikeService articleLikeService;
    private final ArticleFavoriteService articleFavoriteService;
    private final ArticleReadService articleReadService;

    @PostMapping
    public Result<Article> create(@RequestBody Article article) {
        articleService.save(article);
        return Result.ok(article);
    }
    @PostMapping("/batch")
    public Result<Void> createBatch(@RequestBody List<Article> articles) {
        boolean result = articleService.saveBatch(articles);
        return result ? Result.ok() : Result.fail(400, "插入失败");
    }

    @GetMapping
    public Result<IPage<Article>> list(ArticleQueryDTO query) {
        return Result.ok(articleService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                    HttpServletRequest request) {
        Article article = articleService.getById(id);
        articleReadService.recordRead(id, userId, request.getRemoteAddr());
        return Result.ok(article);
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
    @DeleteMapping
    public Result<Void> deleteBatch(@RequestParam List<Long> ids){
        articleService.removeByIds(ids);
        return Result.ok();
    }

    @GetMapping("/statistics/category")
    public Result<List<Map<String, Object>>> categoryStatistics() {
        return Result.ok(articleService.categoryStatistics());
    }

    @PostMapping("/{id}/like")
    public Result<ToggleResult> like(@PathVariable Long id,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.ok(articleLikeService.toggle(id, userId));
    }

    @PostMapping("/{id}/favorite")
    public Result<ToggleResult> favorite(@PathVariable Long id,
                                          @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.ok(articleFavoriteService.toggle(id, userId));
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Integer>> stats(@PathVariable Long id) {
        Article article = articleService.getById(id);
        Map<String, Integer> stats = new HashMap<>();
        stats.put("likeCount", article.getLikeCount());
        stats.put("favoriteCount", article.getFavoriteCount());
        stats.put("readCount", article.getReadCount());
        return Result.ok(stats);
    }

    @GetMapping("/hot")
    public Result<IPage<HotArticleDTO>> hot(@RequestParam(defaultValue = "7") int days,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleReadService.getHotArticles(days, page, size));
    }
}
