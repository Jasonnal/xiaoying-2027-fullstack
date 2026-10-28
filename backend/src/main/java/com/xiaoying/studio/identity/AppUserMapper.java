package com.xiaoying.studio.identity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppUserMapper {

    @Select("SELECT id, handle, display_name, accent_color, created_at FROM app_user WHERE handle = #{handle}")
    AppUser findByHandle(@Param("handle") String handle);

    @Select("SELECT id, handle, display_name, accent_color, created_at FROM app_user WHERE id = #{id}")
    AppUser findById(@Param("id") String id);

    @Select("SELECT id, handle, display_name, accent_color, created_at FROM app_user ORDER BY created_at, handle")
    List<AppUser> findAll();

    @Insert("""
            INSERT INTO app_user (id, handle, display_name, accent_color, created_at)
            VALUES (#{id}, #{handle}, #{displayName}, #{accentColor}, #{createdAt})
            """)
    int insert(AppUser user);
}
