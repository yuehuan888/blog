package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT category, COUNT(*) AS count FROM article GROUP BY category")
    List<Map<String, Object>> countByCategory();

    @Update("UPDATE article SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE article SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Long id);

    @Update("UPDATE article SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    int incrementFavoriteCount(@Param("id") Long id);

    @Update("UPDATE article SET favorite_count = favorite_count - 1 WHERE id = #{id} AND favorite_count > 0")
    int decrementFavoriteCount(@Param("id") Long id);

    @Update("UPDATE article SET read_count = read_count + 1 WHERE id = #{id}")
    int incrementReadCount(@Param("id") Long id);

    @Select("<script>SELECT * FROM article WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Article> selectByIds(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT DISTINCT a.* FROM article a " +
            "INNER JOIN article_tag at ON a.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} " +
            "<if test='category != null and category != \"\"'>AND a.category = #{category}</if> " +
            "<if test='status != null and status != \"\"'>AND a.status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (a.title LIKE CONCAT('%',#{keyword},'%') OR a.content LIKE CONCAT('%',#{keyword},'%')) " +
            "</if> " +
            "ORDER BY a.created_at DESC " +
            "</script>")
    IPage<Article> selectByTagId(Page<Article> page,
                                 @Param("tagId") Long tagId,
                                 @Param("category") String category,
                                 @Param("status") String status,
                                 @Param("keyword") String keyword);

    @Update("UPDATE article SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    @Update("UPDATE article SET comment_count = comment_count - 1 WHERE id = #{id} AND comment_count > 0")
    int decrementCommentCount(@Param("id") Long id);
}
