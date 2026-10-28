package com.xiaoying.studio.job;

/**
 * 任务整体状态。与 {@link JobStage} 分开：状态回答"结果如何"，阶段回答"做到哪一步"。
 */
public enum JobStatus {
    /** 已受理，尚未开始执行 */
    PENDING,
    /** 后台执行中 */
    PROCESSING,
    /** 全部阶段成功 */
    SUCCEEDED,
    /** 任一步失败，后续步骤不再执行 */
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
