package com.blog.controller;

import com.blog.dto.Result;
import com.blog.entity.Danmaku;
import com.blog.mapper.DanmakuMapper;
import com.blog.util.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuMapper danmakuMapper;

    /** Get historical danmaku for a video, optionally since a timestamp. */
    @GetMapping("/{id}/danmaku")
    public Result<List<Danmaku>> getDanmaku(@PathVariable Long id,
                                            @RequestParam(defaultValue = "0") double since) {
        List<Danmaku> list;
        if (since > 0) {
            list = danmakuMapper.selectByArticleIdSince(id, since);
        } else {
            list = danmakuMapper.selectByArticleId(id);
        }
        return Result.ok(list);
    }

    /** Send a danmaku (HTTP fallback when WebSocket is unavailable). */
    @PostMapping("/{id}/danmaku")
    public Result<Danmaku> sendDanmaku(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "Authentication required");
        }

        Danmaku danmaku = new Danmaku();
        danmaku.setArticleId(id);
        danmaku.setUserId(userId);
        danmaku.setContent((String) body.get("content"));
        danmaku.setTimestampSec(((Number) body.get("timestampSec")).doubleValue());
        danmaku.setColor((String) body.getOrDefault("color", "#FFFFFF"));
        danmaku.setMode((String) body.getOrDefault("mode", "scroll"));

        danmakuMapper.insert(danmaku);
        return Result.ok(danmaku);
    }
}
