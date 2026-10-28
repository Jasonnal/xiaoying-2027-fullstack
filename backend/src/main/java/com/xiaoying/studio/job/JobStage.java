package com.xiaoying.studio.job;

/**
 * 后台执行阶段，严格按顺序推进：QUEUED → MATERIAL_CHECK → GENERATE → FINALIZE → DONE。
 * 失败时 stage 停在出错的那一步，并把这一步写进 {@code error_stage}。
 */
public enum JobStage {
    QUEUED("排队中"),
    MATERIAL_CHECK("素材检查"),
    GENERATE("模拟生成"),
    FINALIZE("结果整理"),
    DONE("已完成");

    private final String label;

    JobStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static JobStage fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JobStage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
