package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@org.apache.ibatis.annotations.Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    @Select("SELECT * FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    ArticleLike selectByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
}
