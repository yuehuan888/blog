package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.ToggleResult;
import com.blog.dto.UserDTO;

public interface FollowService {

    ToggleResult toggle(Long followingId);

    IPage<UserDTO> getFollowers(Long userId, int page, int size);

    IPage<UserDTO> getFollowing(Long userId, int page, int size);
}
