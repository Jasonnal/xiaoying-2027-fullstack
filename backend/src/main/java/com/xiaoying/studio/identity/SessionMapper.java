package com.xiaoying.studio.identity;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionMapper {

    @Insert("""
            INSERT INTO app_session (token, user_id, created_at, expires_at)
            VALUES (#{token}, #{userId}, #{createdAt}, #{expiresAt})
            """)
    int insert(@Param("token") String token,
               @Param("userId") String userId,
               @Param("createdAt") LocalDateTime createdAt,
               @Param("expiresAt") LocalDateTime expiresAt);

    @Select("SELECT user_id FROM app_session WHERE token = #{token} AND expires_at > #{now}")
    String findValidUserId(@Param("token") String token, @Param("now") LocalDateTime now);

    @Delete("DELETE FROM app_session WHERE token = #{token}")
    int delete(@Param("token") String token);
}
