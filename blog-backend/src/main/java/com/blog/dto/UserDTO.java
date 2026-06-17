package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private int followerCount;
    private int followingCount;
    private int articleCount;
    private boolean followed;
}
