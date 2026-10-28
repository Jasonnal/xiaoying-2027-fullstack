package com.xiaoying.studio.job;

import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.asset.AssetMapper;
import com.xiaoying.studio.asset.AssetService;
import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.identity.AppUser;
import com.xiaoying.studio.storage.StorageService;
import com.xiaoying.studio.template.Template;
import com.xiaoying.studio.template.TemplateService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class JobService {

    private final JobMapper jobMapper;
    private final JobAssetMapper jobAssetMapper;
    private final JobEventMapper eventMapper;
    private final AssetMapper assetMapper;
    private final AssetService assetService;
    private final TemplateService templateService;
    private final StorageService storageService;
    private final JobDispatcher dispatcher;

    public JobService(JobMapper jobMapper,
                      JobAssetMapper jobAssetMapper,
                      JobEventMapper eventMapper,
                      AssetMapper assetMapper,
                      AssetService assetService,
                      TemplateService templateService,
                      StorageService storageService,
                      JobDispatcher dispatcher) {
        this.jobMapper = jobMapper;
        this.jobAssetMapper = jobAssetMapper;
        this.eventMapper = eventMapper;
        this.assetMapper = assetMapper;
        this.assetService = assetService;
        this.templateService = templateService;
        this.storageService = storageService;
        this.dispatcher = dispatcher;
    }

    /**
     * 受理任务：完成参数与素材校验、落库，然后立即返回。
     * 真正的制作交给后台线程，接口不等结果（R-04）。
     */
    @Transactional
    public JobResponse create(AppUser user, String templateId, List<String> assetIds) {
        Template template = templateService.requireEntity(templateId);
        List<Asset> assets = assetService.requireOwned(assetIds, user);

        if (assets.size() != template.requiredImageCount()) {
            throw ApiException.badRequest("ASSET_COUNT_MISMATCH",
                    "模板《" + template.title() + "》需要 " + template.requiredImageCount()
                            + " 张图片，当前提交 " + assets.size() + " 张");
        }

        List<String> accepted = Arrays.stream(template.acceptedMimeTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (Asset asset : assets) {
            if (!accepted.contains(asset.contentType())) {
                throw ApiException.badRequest("ASSET_TYPE_NOT_ALLOWED",
                        "素材《" + asset.originalName() + "》类型为 " + asset.contentType()
                                + "，该模板只接受 " + String.join(" / ", accepted));
            }
            if (asset.sizeBytes() > template.maxFileSizeBytes()) {
                throw ApiException.badRequest("ASSET_TOO_LARGE",
                        "素材《" + asset.originalName() + "》超过模板上限 "
                                + (template.maxFileSizeBytes() / 1024 / 1024) + " MB");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Job job = new Job(
                UUID.randomUUID().toString(),
                user.id(),
                template.id(),
                template.title(),
                JobStatus.PENDING.name(),
                JobStage.QUEUED.name(),
                0,
                null,
                null,
                null,
                now,
                null,
                null,
                now);
        jobMapper.insert(job);

        for (int i = 0; i < assets.size(); i++) {
            jobAssetMapper.insert(job.id(), assets.get(i).id(), i);
        }
        eventMapper.insert(job.id(), JobStage.QUEUED.name(), "JOB_ACCEPTED", "任务已受理，等待后台处理", now);

        dispatchAfterCommit(job.id());
        return toResponse(job, assets);
    }

    @Transactional(readOnly = true)
    public JobResponse get(AppUser user, String jobId) {
        Job job = jobMapper.findById(jobId);
        if (job == null || !job.userId().equals(user.id())) {
            // 不区分"不存在"和"不属于你"，避免探测他人任务 ID
            throw ApiException.notFound("任务不存在：" + jobId);
        }
        return toResponse(job, loadAssets(job));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list(AppUser user, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Job> jobs = jobMapper.findByUser(user.id(), safeLimit);
        if (jobs.isEmpty()) {
            return List.of();
        }
        Map<String, List<Asset>> assetsByJob = loadAssetsGrouped(jobs);
        List<JobResponse> responses = new ArrayList<>(jobs.size());
        for (Job job : jobs) {
            responses.add(toResponse(job, assetsByJob.getOrDefault(job.id(), List.of())));
        }
        return responses;
    }

    private void dispatchAfterCommit(String jobId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatcher.dispatch(jobId);
            return;
        }
        // 事务提交后再投递给线程池，避免后台线程读不到刚插入的任务
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatcher.dispatch(jobId);
            }
        });
    }

    private JobResponse toResponse(Job job, List<Asset> assets) {
        List<JobAssetResponse> assetResponses = assets.stream()
                .map(asset -> new JobAssetResponse(asset.id(), storageService.urlOf(asset.storagePath())))
                .toList();
        JobStage stage = JobStage.fromString(job.stage());
        return new JobResponse(
                job.id(),
                job.templateId(),
                job.templateTitle(),
                job.status(),
                job.stage(),
                stage == null ? "" : stage.label(),
                job.progress(),
                job.errorStage(),
                job.errorMessage(),
                job.resultUrl(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                assetResponses);
    }

    /** 批量取素材：一次查询覆盖多个任务，历史列表不会出现 N+1。 */
    private Map<String, List<Asset>> loadAssetsGrouped(List<Job> jobs) {
        List<String> jobIds = jobs.stream().map(Job::id).toList();
        List<JobAssetMapper.JobAssetView> views = jobAssetMapper.findByJobIds(jobIds);
        if (views.isEmpty()) {
            return Map.of();
        }
        List<String> assetIds = views.stream().map(JobAssetMapper.JobAssetView::assetId).distinct().toList();
        Map<String, Asset> byId = new HashMap<>();
        for (Asset asset : assetMapper.findByIds(assetIds)) {
            byId.put(asset.id(), asset);
        }
        Map<String, List<Asset>> result = new HashMap<>();
        for (JobAssetMapper.JobAssetView view : views) {
            Asset asset = byId.get(view.assetId());
            if (asset != null) {
                result.computeIfAbsent(view.jobId(), key -> new ArrayList<>()).add(asset);
            }
        }
        return result;
    }

    private List<Asset> loadAssets(Job job) {
        return loadAssetsGrouped(List.of(job)).getOrDefault(job.id(), List.of());
    }

    public record JobAssetResponse(String id, String url) {
    }

    public record JobResponse(
            String id,
            String templateId,
            String templateTitle,
            String status,
            String stage,
            String stageLabel,
            int progress,
            String errorStage,
            String errorMessage,
            String resultUrl,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            List<JobAssetResponse> assets) {
    }
}
