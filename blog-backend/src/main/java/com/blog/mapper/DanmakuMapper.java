package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Danmaku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DanmakuMapper extends BaseMapper<Danmaku> {

    @Select("SELECT * FROM danmaku WHERE article_id = #{articleId} AND timestamp_sec >= #{since} ORDER BY timestamp_sec ASC")
    List<Danmaku> selectByArticleIdSince(@Param("articleId") Long articleId, @Param("since") Double since);

    @Select("SELECT * FROM danmaku WHERE article_id = #{articleId} ORDER BY timestamp_sec ASC")
    List<Danmaku> selectByArticleId(@Param("articleId") Long articleId);
}
