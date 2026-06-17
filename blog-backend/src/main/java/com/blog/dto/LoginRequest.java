package com.blog.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String nickname;

    private String avatar;

    private String password;
}
