package com.xiaoying.studio.job;

import java.time.LocalDateTime;

/**
 * 阶段事件：每一步的开始/完成/失败都会落一条，是"执行边界可观察"的证据。
 */
public record JobEvent(
        Long id,
        String jobId,
        String stage,
        String eventType,
        String message,
        LocalDateTime createdAt) {

    public static final String STAGE_STARTED = "STAGE_STARTED";
    public static final String STAGE_COMPLETED = "STAGE_COMPLETED";
    public static final String STAGE_FAILED = "STAGE_FAILED";
}
