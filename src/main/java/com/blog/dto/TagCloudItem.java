package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagCloudItem {

    private Long id;

    private String name;

    private int articleCount;

    private int hotScore;
}
