package com.blog.controller;

import com.blog.dto.Result;
import com.blog.dto.TagCloudItem;
import com.blog.entity.Tag;
import com.blog.service.ArticleTagService;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final ArticleTagService articleTagService;

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        return Result.ok(tagService.create(tag.getName()));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        return Result.ok(tagService.update(id, tag.getName()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleTagService.deleteByTagId(id);
        tagService.delete(id);
        return Result.ok();
    }

    @GetMapping("/cloud")
    public Result<List<TagCloudItem>> cloud(@RequestParam(defaultValue = "count") String sort) {
        if (!"count".equals(sort) && !"hot".equals(sort)) {
            return Result.fail(400, "sort must be 'count' or 'hot'");
        }
        return Result.ok(tagService.getCloud(sort));
    }
}
