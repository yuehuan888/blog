package com.blog.service;

import com.blog.dto.TagCloudItem;
import com.blog.entity.Tag;

import java.util.List;

public interface TagService {

    Tag create(String name, Long userId);

    Tag update(Long id, String name, Long userId);

    void delete(Long id, Long userId);

    Tag getById(Long id);

    List<TagCloudItem> getCloud(String sort);
}
