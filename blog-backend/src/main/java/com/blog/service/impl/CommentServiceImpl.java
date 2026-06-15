package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.CommentDTO;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;
import com.blog.entity.CommentLike;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.CommentService;
import com.blog.util.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final int PRELOAD_REPLIES = 3;
    private static final String CACHE_COMMENT_PREFIX = "comment:article:";

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.sensitive-words:}")
    private String sensitiveWords;

    @Override
    @Transactional
    public Comment create(Long articleId, Long parentId, Long replyTo, String content) {
        if (articleMapper.selectById(articleId) == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }
        if (parentId != null && commentMapper.selectById(parentId) == null) {
            throw new RuntimeException("Parent comment not found: " + parentId);
        }
        checkSensitiveWords(content);

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(AuthContext.getUserId());
        comment.setParentId(parentId);
        comment.setReplyTo(replyTo);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus("visible");
        commentMapper.insert(comment);

        articleMapper.incrementCommentCount(articleId);
        invalidateCommentCache(articleId);

        return comment;
    }

    @Override
    public IPage<CommentDTO> getTopLevel(Long articleId, int page, int size, String sort) {
        String orderBy = "like".equals(sort) ? "like_count" : "created_at";
        List<Comment> topLevel = commentMapper.selectTopLevel(articleId, orderBy);

        int total = topLevel.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentDTO> records = new ArrayList<>();
        if (fromIndex < total) {
            for (Comment c : topLevel.subList(fromIndex, toIndex)) {
                CommentDTO dto = toDTO(c);
                List<Comment> latestReplies = commentMapper.selectLatestReplies(c.getId(), PRELOAD_REPLIES);
                List<CommentDTO> replyDTOs = new ArrayList<>();
                for (Comment r : latestReplies) {
                    replyDTOs.add(toDTO(r));
                }
                dto.setReplies(replyDTOs);
                dto.setReplyCount(commentMapper.countReplies(c.getId()));
                records.add(dto);
            }
        }

        Page<CommentDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    public IPage<CommentDTO> getReplies(Long parentId, int page, int size) {
        List<Comment> replies = commentMapper.selectReplies(parentId);
        int total = replies.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentDTO> records = new ArrayList<>();
        if (fromIndex < total) {
            for (Comment c : replies.subList(fromIndex, toIndex)) {
                records.add(toDTO(c));
            }
        }

        Page<CommentDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public ToggleResult toggleLike(Long commentId) {
        Long userId = AuthContext.getUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }

        CommentLike existing = commentLikeMapper.selectByCommentAndUser(commentId, userId);
        if (existing != null) {
            commentLikeMapper.deleteByCommentAndUser(commentId, userId);
            commentMapper.decrementLikeCount(commentId);
            return new ToggleResult(false, comment.getLikeCount() - 1);
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            commentMapper.incrementLikeCount(commentId);
            return new ToggleResult(true, comment.getLikeCount() + 1);
        }
    }

    @Override
    @Transactional
    public void delete(Long commentId) {
        Long userId = AuthContext.getUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }

        if (AuthContext.isAdmin()) {
            hardDeleteRecursive(commentId);
        } else if (comment.getUserId().equals(userId)) {
            comment.setStatus("deleted");
            commentMapper.updateById(comment);
            articleMapper.decrementCommentCount(comment.getArticleId());
        } else {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        invalidateCommentCache(comment.getArticleId());
    }

    private void hardDeleteRecursive(Long commentId) {
        List<Comment> children = commentMapper.selectReplies(commentId);
        for (Comment child : children) {
            hardDeleteRecursive(child.getId());
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            commentMapper.deleteById(commentId);
            articleMapper.decrementCommentCount(comment.getArticleId());
        }
    }

    @Override
    @Transactional
    public void hide(Long commentId) {
        if (!AuthContext.isAdmin()) {
            throw new RuntimeException("Admin access required");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }
        comment.setStatus("hidden");
        commentMapper.updateById(comment);
        invalidateCommentCache(comment.getArticleId());
    }

    private void checkSensitiveWords(String content) {
        if (sensitiveWords == null || sensitiveWords.isBlank()) return;
        for (String word : sensitiveWords.split(",")) {
            String w = word.trim();
            if (!w.isEmpty() && content.contains(w)) {
                throw new RuntimeException("Comment contains sensitive word: " + w);
            }
        }
    }

    private CommentDTO toDTO(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setArticleId(c.getArticleId());
        dto.setUserId(c.getUserId());
        dto.setParentId(c.getParentId());
        dto.setReplyTo(c.getReplyTo());
        dto.setContent(c.getContent());
        dto.setLikeCount(c.getLikeCount());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setReplies(List.of());
        dto.setReplyCount(0);

        // Check if current user liked this comment
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null) {
            dto.setLiked(commentLikeMapper.selectByCommentAndUser(c.getId(), currentUserId) != null);
        }

        return dto;
    }

    private void invalidateCommentCache(Long articleId) {
        if (redisTemplate == null) return;
        try {
            String pattern = CACHE_COMMENT_PREFIX + articleId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate comment cache: articleId={}", articleId, e);
        }
    }
}
