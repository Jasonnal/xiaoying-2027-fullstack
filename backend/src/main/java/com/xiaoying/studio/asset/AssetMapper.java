package com.xiaoying.studio.asset;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssetMapper {

    String COLUMNS = "id, user_id, original_name, content_type, size_bytes, storage_path, created_at";

    @Insert("""
            INSERT INTO asset (id, user_id, original_name, content_type, size_bytes, storage_path, created_at)
            VALUES (#{id}, #{userId}, #{originalName}, #{contentType}, #{sizeBytes}, #{storagePath}, #{createdAt})
            """)
    int insert(Asset asset);

    @Select("SELECT " + COLUMNS + " FROM asset WHERE id = #{id}")
    Asset findById(@Param("id") String id);

    @Select("<script>SELECT " + COLUMNS + " FROM asset WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Asset> findByIds(@Param("ids") List<String> ids);
}
