package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.CommentLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
