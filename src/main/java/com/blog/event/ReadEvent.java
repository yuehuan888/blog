package com.blog.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadEvent {
    private Long articleId;
    private Long userId;
    private String ip;
}
