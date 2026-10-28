package com.xiaoying.studio.job;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobAssetMapper {

    @Insert("""
            INSERT INTO job_asset (job_id, asset_id, position)
            VALUES (#{jobId}, #{assetId}, #{position})
            """)
    int insert(@Param("jobId") String jobId,
               @Param("assetId") String assetId,
               @Param("position") int position);

    /** 一次取回多个任务的素材，避免历史列表里 N+1 查询。 */
    @Select("<script>"
            + "SELECT ja.job_id AS job_id, a.id AS asset_id, ja.position AS position, a.storage_path AS storage_path "
            + "FROM job_asset ja JOIN asset a ON a.id = ja.asset_id "
            + "WHERE ja.job_id IN "
            + "<foreach collection='jobIds' item='jobId' open='(' separator=',' close=')'>#{jobId}</foreach> "
            + "ORDER BY ja.job_id, ja.position"
            + "</script>")
    List<JobAssetView> findByJobIds(@Param("jobIds") List<String> jobIds);

    record JobAssetView(String jobId, String assetId, int position, String storagePath) {
    }
}
