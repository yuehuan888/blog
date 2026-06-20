package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT * FROM tag WHERE name = #{name}")
    Tag selectByName(@Param("name") String name);

    @Select("SELECT * FROM tag ORDER BY article_count DESC")
    List<Tag> selectByArticleCountDesc();

    @Select("SELECT t.*, COALESCE(SUM(a.read_count), 0) AS hot_score " +
            "FROM tag t " +
            "LEFT JOIN article_tag at ON t.id = at.tag_id " +
            "LEFT JOIN article a ON at.article_id = a.id " +
            "GROUP BY t.id " +
            "ORDER BY hot_score DESC")
    List<Tag> selectWithHotScore();

    @Update("UPDATE tag SET article_count = article_count + 1 WHERE id = #{id}")
    int incrementArticleCount(@Param("id") Long id);

    @Update("UPDATE tag SET article_count = article_count - 1 WHERE id = #{id} AND article_count > 0")
    int decrementArticleCount(@Param("id") Long id);

    /** 标签云按已发布文章数排序（实时 JOIN，不依赖 tag.article_count） */
    @Select("SELECT t.id, t.name, " +
            "COALESCE(COUNT(CASE WHEN a.status = 'published' THEN 1 END), 0) AS article_count, " +
            "t.hot_score, t.created_at " +
            "FROM tag t " +
            "LEFT JOIN article_tag at2 ON t.id = at2.tag_id " +
            "LEFT JOIN article a ON at2.article_id = a.id " +
            "GROUP BY t.id " +
            "ORDER BY article_count DESC")
    List<Tag> selectWithPublishedArticleCount();

    /** 校正 tag.article_count 为真实已发布文章数 */
    @Select("SELECT COUNT(*) FROM article_tag at2 " +
            "JOIN article a ON at2.article_id = a.id " +
            "WHERE at2.tag_id = #{tagId} AND a.status = 'published'")
    int countPublishedArticles(@Param("tagId") Long tagId);
}
