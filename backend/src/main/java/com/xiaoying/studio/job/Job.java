package com.xiaoying.studio.job;

import java.time.LocalDateTime;

/**
 * 制作任务。status/stage 都用字符串落库，便于后续扩展枚举值而不用改表。
 */
public record Job(
        String id,
        String userId,
        String templateId,
        String templateTitle,
        String status,
        String stage,
        int progress,
        String errorStage,
        String errorMessage,
        String resultUrl,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime updatedAt) {

    public boolean isTerminal() {
        return JobStatus.SUCCEEDED.name().equals(status) || JobStatus.FAILED.name().equals(status);
    }
}
