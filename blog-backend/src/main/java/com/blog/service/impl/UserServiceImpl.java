package com.blog.service.impl;

import com.blog.dto.LoginResponse;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.UserService;
import com.blog.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final Duration TOKEN_EXPIRY = Duration.ofDays(1);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public LoginResponse register(String username, String password, String nickname) {
        if (userMapper.selectByUsername(username) != null) {
            throw new RuntimeException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setPassword(password);
        user.setRole("user");
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
    }

    @Override
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
    }

    @Override
    public void logout(String token) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", TOKEN_EXPIRY);
        } catch (Exception e) {
            log.warn("Failed to blacklist token", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist", e);
            return false;
        }
    }
}
