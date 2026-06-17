package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ToggleResult;
import com.blog.dto.UserDTO;
import com.blog.entity.Follow;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.FollowService;
import com.blog.util.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    @Transactional
    public ToggleResult toggle(Long followingId) {
        Long userId = AuthContext.getUserId();
        if (userId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself");
        }
        User target = userMapper.selectById(followingId);
        if (target == null) {
            throw new RuntimeException("User not found: " + followingId);
        }

        Follow existing = followMapper.selectByFollowerAndFollowing(userId, followingId);
        if (existing != null) {
            followMapper.deleteByFollowerAndFollowing(userId, followingId);
            int count = followMapper.countFollowers(followingId);
            return new ToggleResult(false, count);
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(userId);
            follow.setFollowingId(followingId);
            followMapper.insert(follow);
            int count = followMapper.countFollowers(followingId);
            return new ToggleResult(true, count);
        }
    }

    @Override
    public IPage<UserDTO> getFollowers(Long userId, int page, int size) {
        // For simplicity, skip pagination for followers — return all
        List<Follow> follows = followMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowingId, userId)
                        .orderByDesc(Follow::getCreatedAt));
        List<UserDTO> dtos = new ArrayList<>();
        for (Follow f : follows) {
            User u = userMapper.selectById(f.getFollowerId());
            if (u != null) {
                dtos.add(toUserDTO(u));
            }
        }
        Page<UserDTO> result = new Page<>(page, size, dtos.size());
        result.setRecords(dtos);
        return result;
    }

    @Override
    public IPage<UserDTO> getFollowing(Long userId, int page, int size) {
        List<Follow> follows = followMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .orderByDesc(Follow::getCreatedAt));
        List<UserDTO> dtos = new ArrayList<>();
        for (Follow f : follows) {
            User u = userMapper.selectById(f.getFollowingId());
            if (u != null) {
                dtos.add(toUserDTO(u));
            }
        }
        Page<UserDTO> result = new Page<>(page, size, dtos.size());
        result.setRecords(dtos);
        return result;
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setFollowerCount(followMapper.countFollowers(user.getId()));
        dto.setFollowingCount(followMapper.countFollowing(user.getId()));
        dto.setArticleCount(articleMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.blog.entity.Article>()
                        .eq(com.blog.entity.Article::getAuthorId, user.getId())
                        .eq(com.blog.entity.Article::getStatus, "published")).intValue());
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && !currentUserId.equals(user.getId())) {
            dto.setFollowed(followMapper.selectByFollowerAndFollowing(currentUserId, user.getId()) != null);
        }
        return dto;
    }
}
