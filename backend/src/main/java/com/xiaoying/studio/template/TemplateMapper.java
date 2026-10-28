package com.xiaoying.studio.template;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper {

    String COLUMNS = "id, title, category, cover_url, description, required_image_count, "
            + "accepted_mime_types, max_file_size_bytes, sort_order, created_at";

    @Select("SELECT " + COLUMNS + " FROM template WHERE id = #{id}")
    Template findById(@Param("id") String id);

    @Select("<script>SELECT " + COLUMNS + " FROM template "
            + "<where><if test='category != null and category != \"\"'> category = #{category}</if></where>"
            + " ORDER BY sort_order, title</script>")
    List<Template> findAll(@Param("category") String category);

    @Select("SELECT DISTINCT category FROM template ORDER BY category")
    List<String> findCategories();

    @Select("SELECT COUNT(*) FROM template")
    int count();

    @Insert("""
            INSERT INTO template (id, title, category, cover_url, description, required_image_count,
                                  accepted_mime_types, max_file_size_bytes, sort_order, created_at)
            VALUES (#{id}, #{title}, #{category}, #{coverUrl}, #{description}, #{requiredImageCount},
                    #{acceptedMimeTypes}, #{maxFileSizeBytes}, #{sortOrder}, #{createdAt})
            """)
    int insert(Template template);
}
