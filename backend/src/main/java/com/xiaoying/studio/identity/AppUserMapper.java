package com.xiaoying.studio.identity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AppUserMapper {

    String COLUMNS = "id, handle, display_name, password_hash, accent_color, created_at";

    @Select("SELECT " + COLUMNS + " FROM app_user WHERE handle = #{handle}")
    AppUser findByHandle(@Param("handle") String handle);

    @Select("SELECT " + COLUMNS + " FROM app_user WHERE id = #{id}")
    AppUser findById(@Param("id") String id);

    @Select("SELECT " + COLUMNS + " FROM app_user ORDER BY created_at, handle")
    List<AppUser> findAll();

    @Insert("""
            INSERT INTO app_user (id, handle, display_name, password_hash, accent_color, created_at)
            VALUES (#{id}, #{handle}, #{displayName}, #{passwordHash}, #{accentColor}, #{createdAt})
            """)
    int insert(AppUser user);

    /** 演示账号补口令 / 后续改密使用。 */
    @Update("UPDATE app_user SET password_hash = #{passwordHash} WHERE id = #{id}")
    int updatePassword(@Param("id") String id, @Param("passwordHash") String passwordHash);
}
