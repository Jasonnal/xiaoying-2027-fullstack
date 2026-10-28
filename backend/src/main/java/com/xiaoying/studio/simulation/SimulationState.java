package com.xiaoying.studio.simulation;

import com.xiaoying.studio.config.AppProperties;
import com.xiaoying.studio.job.JobStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * 运行时可调的模拟器开关（全局单例，线程安全）。
 *
 * <p>题目不要求随机失败，这里做成"确定性 + 可重复"：
 * 指定阶段（MATERIAL_CHECK / GENERATE / FINALIZE），并指定接下来几个任务在该阶段失败
 * （{@code failCount = -1} 表示一直失败，0 表示恢复正常）。每次失败消耗一次配额，
 * 因此"同样操作再来一遍"就能复现同一个失败。
 */
@Component
public class SimulationState {

    private volatile JobStage failStage = JobStage.QUEUED;
    private volatile long stageDelayMs;
    private final AtomicInteger remainingFailures = new AtomicInteger(0);
    private final long defaultStageDelayMs;

    public SimulationState(AppProperties properties) {
        this.defaultStageDelayMs = Math.max(0, properties.pipeline().stageDelayMs());
        this.stageDelayMs = this.defaultStageDelayMs;
        apply(properties.simulation().failStage(), properties.simulation().failCount());
    }

    public synchronized void apply(String failStage, int failCount) {
        JobStage parsed = JobStage.fromString(failStage);
        this.failStage = parsed == null ? JobStage.QUEUED : parsed;
        this.remainingFailures.set(Math.max(-1, failCount));
        if (this.failStage == JobStage.QUEUED || this.failStage == JobStage.DONE) {
            // QUEUED/DONE 不是可失败阶段，视为关闭
            this.remainingFailures.set(0);
        }
    }

    public void setStageDelayMs(long delayMs) {
        this.stageDelayMs = Math.max(0, Math.min(60_000, delayMs));
    }

    /** 当前阶段是否应当立即失败；消耗一次配额。 */
    public boolean shouldFail(JobStage stage) {
        if (stage != failStage) {
            return false;
        }
        int remaining = remainingFailures.get();
        if (remaining == 0) {
            return false;
        }
        if (remaining > 0) {
            remainingFailures.decrementAndGet();
        }
        return true;
    }

    public long stageDelayMs() {
        return stageDelayMs;
    }

    public void reset() {
        this.failStage = JobStage.QUEUED;
        this.remainingFailures.set(0);
        this.stageDelayMs = defaultStageDelayMs;
    }

    public Snapshot snapshot() {
        return new Snapshot(failStage.name(), remainingFailures.get(), stageDelayMs);
    }

    public record Snapshot(String failStage, int remainingFailures, long stageDelayMs) {
    }
}
