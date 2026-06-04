package com.blog.service;

import com.blog.dto.LoginResponse;

public interface UserService {

    LoginResponse register(String username, String password);

    LoginResponse login(String username, String password);

    void logout(String token);
}
