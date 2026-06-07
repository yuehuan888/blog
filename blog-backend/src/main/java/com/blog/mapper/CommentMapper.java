package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 'visible' ORDER BY ${orderBy} DESC")
    List<Comment> selectTopLevel(@Param("articleId") Long articleId, @Param("orderBy") String orderBy);

    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 'visible' ORDER BY created_at ASC")
    List<Comment> selectReplies(@Param("parentId") Long parentId);

    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 'visible' ORDER BY created_at DESC LIMIT #{limit}")
    List<Comment> selectLatestReplies(@Param("parentId") Long parentId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM comment WHERE parent_id = #{parentId} AND status = 'visible'")
    int countReplies(@Param("parentId") Long parentId);

    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE comment SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Long id);
}
