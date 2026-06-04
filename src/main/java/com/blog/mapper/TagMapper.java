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
}
