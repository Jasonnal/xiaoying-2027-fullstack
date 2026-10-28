package com.xiaoying.studio.job;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface JobMapper {

    String COLUMNS = "id, user_id, template_id, template_title, status, stage, progress, "
            + "error_stage, error_message, result_url, created_at, started_at, finished_at, updated_at";

    @Insert("""
            INSERT INTO creation_job (id, user_id, template_id, template_title, status, stage, progress,
                                      error_stage, error_message, result_url,
                                      created_at, started_at, finished_at, updated_at)
            VALUES (#{id}, #{userId}, #{templateId}, #{templateTitle}, #{status}, #{stage}, #{progress},
                    #{errorStage}, #{errorMessage}, #{resultUrl},
                    #{createdAt}, #{startedAt}, #{finishedAt}, #{updatedAt})
            """)
    int insert(Job job);

    @Select("SELECT " + COLUMNS + " FROM creation_job WHERE id = #{id}")
    Job findById(@Param("id") String id);

    @Select("SELECT " + COLUMNS + " FROM creation_job WHERE user_id = #{userId} "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<Job> findByUser(@Param("userId") String userId, @Param("limit") int limit);

    /** 只有 PENDING 才能进入 PROCESSING，重复投递不会把同一任务跑两遍。 */
    @Update("UPDATE creation_job SET status = 'PROCESSING', started_at = #{now}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markStarted(@Param("id") String id, @Param("now") LocalDateTime now);

    /** 只允许在 PROCESSING 状态下推进阶段，终态任务不会被改回处理中。 */
    @Update("UPDATE creation_job SET stage = #{stage}, progress = #{progress}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PROCESSING'")
    int updateStage(@Param("id") String id,
                    @Param("stage") String stage,
                    @Param("progress") int progress,
                    @Param("now") LocalDateTime now);

    @Update("UPDATE creation_job SET status = 'SUCCEEDED', stage = 'DONE', progress = 100, "
            + "result_url = #{resultUrl}, finished_at = #{now}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PROCESSING'")
    int markSucceeded(@Param("id") String id,
                      @Param("resultUrl") String resultUrl,
                      @Param("now") LocalDateTime now);

    @Update("UPDATE creation_job SET status = 'FAILED', error_stage = #{stage}, error_message = #{message}, "
            + "finished_at = #{now}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PROCESSING'")
    int markFailed(@Param("id") String id,
                   @Param("stage") String stage,
                   @Param("message") String message,
                   @Param("now") LocalDateTime now);
}
