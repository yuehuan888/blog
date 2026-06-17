package com.blog.service;

import com.blog.dto.LoginResponse;

public interface UserService {

    LoginResponse register(String username, String password, String nickname, String avatar);

    LoginResponse login(String username, String password);

    void logout(String token);
}
