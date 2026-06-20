package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.Result;
import com.blog.dto.ToggleResult;
import com.blog.dto.UserDTO;
import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.FollowService;
import com.blog.util.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final ArticleMapper articleMapper;
    private final FollowService followService;

    @GetMapping("/users/{id}")
    public Result<UserDTO> getProfile(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(404, "User not found");
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setFollowerCount(followMapper.countFollowers(id));
        dto.setFollowingCount(followMapper.countFollowing(id));
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getAuthorId, id)
                        .eq(Article::getStatus, "published"));
        dto.setArticleCount(articleCount != null ? articleCount.intValue() : 0);

        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && !currentUserId.equals(id)) {
            dto.setFollowed(followMapper.selectByFollowerAndFollowing(currentUserId, id) != null);
        }

        return Result.ok(dto);
    }

    @PutMapping("/users/profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "Authentication required");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "User not found");
        }

        String nickname = body.get("nickname");
        String avatar = body.get("avatar");

        if (nickname != null) {
            if (nickname.isBlank()) {
                return Result.fail(400, "昵称不能为空");
            }
            user.setNickname(nickname.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar.trim());
        }

        userMapper.updateById(user);

        return Result.ok(Map.of(
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    @PostMapping("/users/{id}/follow")
    public Result<ToggleResult> follow(@PathVariable Long id) {
        return Result.ok(followService.toggle(id));
    }

    @GetMapping("/users/{id}/followers")
    public Result<IPage<UserDTO>> getFollowers(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(followService.getFollowers(id, page, size));
    }

    @GetMapping("/users/{id}/following")
    public Result<IPage<UserDTO>> getFollowing(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(followService.getFollowing(id, page, size));
    }
}
