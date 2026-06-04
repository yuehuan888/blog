package com.blog.service;

import com.blog.dto.TagCloudItem;
import com.blog.entity.Tag;

import java.util.List;

public interface TagService {

    Tag create(String name);

    Tag update(Long id, String name);

    void delete(Long id);

    Tag getById(Long id);

    List<TagCloudItem> getCloud(String sort);
}
