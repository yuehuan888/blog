package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleRead;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@org.apache.ibatis.annotations.Mapper
public interface ArticleReadMapper extends BaseMapper<ArticleRead> {

    @Select("SELECT article_id, COUNT(*) AS cnt FROM article_read WHERE created_at > DATE_SUB(NOW(), INTERVAL #{days} DAY) GROUP BY article_id ORDER BY cnt DESC")
    List<Map<String, Object>> countReadsByArticleInRange(@Param("days") int days);
}
