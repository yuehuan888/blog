package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.CommentDTO;
import com.blog.dto.Result;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comments")
    public Result<Comment> create(@RequestBody Comment comment) {
        return Result.ok(commentService.create(
                comment.getArticleId(),
                comment.getParentId(),
                comment.getReplyTo(),
                comment.getContent()));
    }

    @GetMapping("/articles/{id}/comments")
    public Result<IPage<CommentDTO>> getTopLevel(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "time") String sort) {
        if (!"time".equals(sort) && !"like".equals(sort)) {
            return Result.fail(400, "sort must be 'time' or 'like'");
        }
        return Result.ok(commentService.getTopLevel(id, page, size, sort));
    }

    @GetMapping("/comments/{id}/replies")
    public Result<IPage<CommentDTO>> getReplies(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(commentService.getReplies(id, page, size));
    }

    @PostMapping("/comments/{id}/like")
    public Result<ToggleResult> like(@PathVariable Long id) {
        return Result.ok(commentService.toggleLike(id));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @PutMapping("/comments/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        commentService.hide(id);
        return Result.ok();
    }
}
