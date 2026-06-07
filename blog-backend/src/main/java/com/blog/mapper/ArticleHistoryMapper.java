package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleHistoryMapper extends BaseMapper<ArticleHistory> {

    @Select("SELECT * FROM article_history WHERE article_id = #{articleId} ORDER BY version_no DESC")
    List<ArticleHistory> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM article_history WHERE article_id = #{articleId}")
    int getMaxVersionNo(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_history WHERE article_id = #{articleId} " +
            "AND id NOT IN (SELECT * FROM (" +
            "  SELECT id FROM article_history WHERE article_id = #{articleId} " +
            "  ORDER BY version_no DESC LIMIT #{keepCount}" +
            ") t)")
    int deleteOldVersions(@Param("articleId") Long articleId, @Param("keepCount") int keepCount);
}
