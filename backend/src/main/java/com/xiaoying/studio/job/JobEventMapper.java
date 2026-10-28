package com.xiaoying.studio.job;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobEventMapper {

    @Insert("""
            INSERT INTO job_event (job_id, stage, event_type, message, created_at)
            VALUES (#{jobId}, #{stage}, #{eventType}, #{message}, #{createdAt})
            """)
    int insert(@Param("jobId") String jobId,
               @Param("stage") String stage,
               @Param("eventType") String eventType,
               @Param("message") String message,
               @Param("createdAt") LocalDateTime createdAt);

    @Select("SELECT id, job_id, stage, event_type, message, created_at FROM job_event "
            + "WHERE job_id = #{jobId} ORDER BY id")
    List<JobEvent> findByJobId(@Param("jobId") String jobId);
}
