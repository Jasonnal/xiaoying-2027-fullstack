package com.xiaoying.studio.job;

import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.asset.AssetMapper;
import com.xiaoying.studio.render.ResultComposer;
import com.xiaoying.studio.simulation.SimulationState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.xiaoying.studio.storage.StorageService;

/**
 * 三段流水线：素材检查 → 模拟生成 → 结果整理。
 *
 * <p>几个刻意的设计：
 * <ul>
 *   <li>每次推进阶段都先写库再做事，前端轮询能看到"正在哪一步"，而不是一步跳到结果；</li>
 *   <li>任何一步抛错都只写一次 FAILED，并记录失败阶段与原因，后续步骤不再执行；</li>
 *   <li>只有查询接口读库，没有任何查询会触发状态推进（R-05 的硬要求）。</li>
 * </ul>
 */
@Component
public class JobPipeline {

    private static final Logger log = LoggerFactory.getLogger(JobPipeline.class);

    private final JobMapper jobMapper;
    private final JobEventMapper eventMapper;
    private final JobAssetMapper jobAssetMapper;
    private final AssetMapper assetMapper;
    private final StorageService storageService;
    private final ResultComposer resultComposer;
    private final SimulationState simulation;

    public JobPipeline(JobMapper jobMapper,
                       JobEventMapper eventMapper,
                       JobAssetMapper jobAssetMapper,
                       AssetMapper assetMapper,
                       StorageService storageService,
                       ResultComposer resultComposer,
                       SimulationState simulation) {
        this.jobMapper = jobMapper;
        this.eventMapper = eventMapper;
        this.jobAssetMapper = jobAssetMapper;
        this.assetMapper = assetMapper;
        this.storageService = storageService;
        this.resultComposer = resultComposer;
        this.simulation = simulation;
    }

    public void run(String jobId) {
        Job job = jobMapper.findById(jobId);
        if (job == null || job.isTerminal()) {
            return;
        }
        // 抢占式启动：只有 PENDING → PROCESSING 成功的那次执行才继续，重复投递不会跑两遍
        if (jobMapper.markStarted(jobId, LocalDateTime.now()) == 0) {
            return;
        }

        List<Asset> assets = loadAssets(jobId);
        JobStage current = JobStage.QUEUED;
        try {
            current = JobStage.MATERIAL_CHECK;
            startStage(jobId, current, 10);
            pause();
            failIfRequested(current);
            verifyAssets(assets);
            completeStage(jobId, current, 30);

            current = JobStage.GENERATE;
            startStage(jobId, current, 40);
            pause();
            failIfRequested(current);
            simulateGeneration(job, assets);
            completeStage(jobId, current, 75);

            current = JobStage.FINALIZE;
            startStage(jobId, current, 80);
            pause();
            failIfRequested(current);
            String resultUrl = resultComposer.compose(job, assets);
            completeStage(jobId, current, 95);

            jobMapper.markSucceeded(jobId, resultUrl, LocalDateTime.now());
            log.info("任务 {} 完成：{}", jobId, resultUrl);
        } catch (StageFailure failure) {
            failJob(jobId, failure.stage(), failure.getMessage());
        } catch (RuntimeException ex) {
            log.error("任务 {} 在阶段 {} 异常失败", jobId, current, ex);
            failJob(jobId, current, "内部错误：" + ex.getClass().getSimpleName());
        }
    }

    private void startStage(String jobId, JobStage stage, int progress) {
        jobMapper.updateStage(jobId, stage.name(), progress, LocalDateTime.now());
        eventMapper.insert(jobId, stage.name(), JobEvent.STAGE_STARTED, null, LocalDateTime.now());
    }

    private void completeStage(String jobId, JobStage stage, int progress) {
        jobMapper.updateStage(jobId, stage.name(), progress, LocalDateTime.now());
        eventMapper.insert(jobId, stage.name(), JobEvent.STAGE_COMPLETED, null, LocalDateTime.now());
    }

    private void failIfRequested(JobStage stage) {
        if (simulation.shouldFail(stage)) {
            throw new StageFailure(stage, failureMessage(stage));
        }
    }

    private String failureMessage(JobStage stage) {
        return switch (stage) {
            case MATERIAL_CHECK -> "素材检查未通过（模拟）：源图尺寸低于模板要求";
            case GENERATE -> "模拟生成失败（模拟）：渲染队列返回 500";
            case FINALIZE -> "结果整理失败（模拟）：写入结果文件时磁盘错误";
            default -> "模拟失败";
        };
    }

    /** 模拟生成：不接 AI，只消耗可配置的耗时，保证阶段边界真实存在。 */
    private void simulateGeneration(Job job, List<Asset> assets) {
        log.info("任务 {} 开始模拟生成，模板 {}，素材 {} 张", job.id(), job.templateId(), assets.size());
    }

    /** 素材检查做的是真检查：文件必须还在磁盘上且非空。 */
    private void verifyAssets(List<Asset> assets) {
        if (assets.isEmpty()) {
            throw new StageFailure(JobStage.MATERIAL_CHECK, "任务没有关联任何素材");
        }
        for (Asset asset : assets) {
            Path path = storageService.resolve(asset.storagePath());
            if (!Files.exists(path)) {
                throw new StageFailure(JobStage.MATERIAL_CHECK, "素材文件丢失：" + asset.originalName());
            }
            try {
                if (Files.size(path) == 0) {
                    throw new StageFailure(JobStage.MATERIAL_CHECK, "素材文件为空：" + asset.originalName());
                }
            } catch (java.io.IOException e) {
                throw new StageFailure(JobStage.MATERIAL_CHECK, "素材文件不可读：" + asset.originalName());
            }
        }
    }

    private void pause() {
        long delay = simulation.stageDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StageFailure(JobStage.QUEUED, "任务被中断");
        }
    }

    private void failJob(String jobId, JobStage stage, String message) {
        String safe = message == null ? "未知错误" : message.substring(0, Math.min(message.length(), 480));
        jobMapper.markFailed(jobId, stage.name(), safe, LocalDateTime.now());
        eventMapper.insert(jobId, stage.name(), JobEvent.STAGE_FAILED, safe, LocalDateTime.now());
        log.info("任务 {} 失败于 {}：{}", jobId, stage, safe);
    }

    private List<Asset> loadAssets(String jobId) {
        List<JobAssetMapper.JobAssetView> views = jobAssetMapper.findByJobIds(List.of(jobId));
        if (views.isEmpty()) {
            return List.of();
        }
        List<String> ids = views.stream().map(JobAssetMapper.JobAssetView::assetId).toList();
        Map<String, Asset> byId = new HashMap<>();
        for (Asset asset : assetMapper.findByIds(ids)) {
            byId.put(asset.id(), asset);
        }
        List<Asset> ordered = new ArrayList<>(views.size());
        for (JobAssetMapper.JobAssetView view : views) {
            Asset asset = byId.get(view.assetId());
            if (asset != null) {
                ordered.add(asset);
            }
        }
        return ordered;
    }

    /** 阶段失败：带上失败阶段，便于落库后前端直接展示。 */
    static class StageFailure extends RuntimeException {
        private final JobStage stage;

        StageFailure(JobStage stage, String message) {
            super(message);
            this.stage = stage;
        }

        JobStage stage() {
            return stage;
        }
    }
}
