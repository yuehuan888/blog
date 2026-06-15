package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@org.apache.ibatis.annotations.Mapper
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite> {

    @Select("SELECT * FROM article_favorite WHERE user_id = #{userId} AND article_id = #{articleId}")
    ArticleFavorite selectByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Delete("DELETE FROM article_favorite WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Delete("DELETE FROM article_favorite WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
