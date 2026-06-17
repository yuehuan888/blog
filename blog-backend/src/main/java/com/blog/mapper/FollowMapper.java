package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    Follow selectByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE following_id = #{userId}")
    int countFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{userId}")
    int countFollowing(@Param("userId") Long userId);

    @Delete("DELETE FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int deleteByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}
