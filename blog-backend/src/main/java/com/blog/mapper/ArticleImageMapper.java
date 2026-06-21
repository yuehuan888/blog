package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleImageMapper extends BaseMapper<ArticleImage> {

    @Delete("DELETE FROM article_image WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM article_image WHERE article_id = #{articleId} ORDER BY sort_order")
    List<ArticleImage> selectByArticleId(@Param("articleId") Long articleId);

    @Select("<script>SELECT article_id, COUNT(*) AS cnt FROM article_image WHERE article_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY article_id</script>")
    List<Map<String, Object>> countByArticleIds(@Param("ids") List<Long> articleIds);

    @Select("<script>SELECT * FROM article_image WHERE article_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND sort_order = 0</script>")
    List<ArticleImage> selectCoverImages(@Param("ids") List<Long> articleIds);
}
