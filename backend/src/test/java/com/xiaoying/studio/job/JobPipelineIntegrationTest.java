package com.xiaoying.studio.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.asset.AssetMapper;
import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.identity.AppUser;
import com.xiaoying.studio.identity.AppUserMapper;
import com.xiaoying.studio.identity.AuthService;
import com.xiaoying.studio.simulation.SimulationState;
import com.xiaoying.studio.storage.StorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 流水线的核心行为测试：阶段顺序、失败中断、终态不可回退、受理校验。
 *
 * <p>测试 profile 里执行器被换成同步实现，因此 {@code jobService.create()} 返回时
 * 任务已经跑完，可以同步断言数据库里的最终状态与事件序列。
 */
@SpringBootTest
@ActiveProfiles("test")
class JobPipelineIntegrationTest {

    /** 1×1 PNG，够 ImageIO 识别，也够"结果整理"真的写出一张图。 */
    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private JobService jobService;
    @Autowired
    private JobPipeline jobPipeline;
    @Autowired
    private JobMapper jobMapper;
    @Autowired
    private JobEventMapper jobEventMapper;
    @Autowired
    private AssetMapper assetMapper;
    @Autowired
    private AppUserMapper userMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private SimulationState simulationState;
    @Autowired
    private StorageService storageService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM job_event");
        jdbcTemplate.update("DELETE FROM job_asset");
        jdbcTemplate.update("DELETE FROM creation_job");
        jdbcTemplate.update("DELETE FROM asset");
        simulationState.reset();
        owner = userMapper.findByHandle("demo");
        assertThat(owner).as("演示账号应由 seeder 初始化").isNotNull();
    }

    @Test
    @DisplayName("三段流水线按顺序执行，成功后写出结果图")
    void runsStagesInOrderAndProducesResult() throws Exception {
        Asset first = newAsset("first.png");
        Asset second = newAsset("second.png");

        JobService.JobResponse accepted = jobService.create(owner, "travel-postcard",
                List.of(first.id(), second.id()));

        // 受理时返回的是快照，此时还没开始跑：接口本身不等结果
        assertThat(accepted.status()).isEqualTo(JobStatus.PENDING.name());
        assertThat(accepted.stage()).isEqualTo(JobStage.QUEUED.name());

        Job stored = jobMapper.findById(accepted.id());
        assertThat(stored.status()).isEqualTo(JobStatus.SUCCEEDED.name());
        assertThat(stored.stage()).isEqualTo(JobStage.DONE.name());
        assertThat(stored.resultUrl()).isEqualTo("/api/files/results/" + accepted.id() + ".jpg");
        assertThat(stored.finishedAt()).isNotNull();

        Path resultFile = storageService.resolve("results/" + accepted.id() + ".jpg");
        assertThat(Files.exists(resultFile)).isTrue();
        assertThat(Files.size(resultFile)).isGreaterThan(0);

        List<JobEvent> events = jobEventMapper.findByJobId(accepted.id());
        assertThat(events).extracting(JobEvent::eventType).containsExactly(
                "JOB_ACCEPTED",
                JobEvent.STAGE_STARTED, JobEvent.STAGE_COMPLETED,
                JobEvent.STAGE_STARTED, JobEvent.STAGE_COMPLETED,
                JobEvent.STAGE_STARTED, JobEvent.STAGE_COMPLETED);
        assertThat(events.subList(1, 7)).extracting(JobEvent::stage).containsExactly(
                "MATERIAL_CHECK", "MATERIAL_CHECK",
                "GENERATE", "GENERATE",
                "FINALIZE", "FINALIZE");
    }

    @Test
    @DisplayName("在模拟生成阶段失败后立即停止，不再进入结果整理")
    void failureStopsFollowingStages() {
        simulationState.apply("GENERATE", 1);
        Asset first = newAsset("first.png");
        Asset second = newAsset("second.png");

        JobService.JobResponse accepted = jobService.create(owner, "travel-postcard",
                List.of(first.id(), second.id()));

        Job stored = jobMapper.findById(accepted.id());
        assertThat(stored.status()).isEqualTo(JobStatus.FAILED.name());
        assertThat(stored.stage()).isEqualTo(JobStage.GENERATE.name());
        assertThat(stored.errorStage()).isEqualTo(JobStage.GENERATE.name());
        assertThat(stored.errorMessage()).contains("模拟生成失败");
        assertThat(stored.resultUrl()).isNull();
        assertThat(stored.finishedAt()).isNotNull();

        List<JobEvent> events = jobEventMapper.findByJobId(accepted.id());
        assertThat(events).extracting(JobEvent::stage).doesNotContain("FINALIZE");
        assertThat(events.get(events.size() - 1).eventType()).isEqualTo(JobEvent.STAGE_FAILED);

        // 失败只消耗一次配额：下一次不再失败，说明模拟是可控且可重复的
        assertThat(simulationState.shouldFail(JobStage.GENERATE)).isFalse();
    }

    @Test
    @DisplayName("已经完成的任务不会被再次执行，也不会退回处理中")
    void terminalJobIsNotRerunNorReverted() {
        Asset first = newAsset("first.png");
        Asset second = newAsset("second.png");
        JobService.JobResponse accepted = jobService.create(owner, "travel-postcard",
                List.of(first.id(), second.id()));

        // 把模拟器调成"必失败"，再跑一次：终态任务应该被 markStarted 拦下
        simulationState.apply("MATERIAL_CHECK", -1);
        jobPipeline.run(accepted.id());

        Job stored = jobMapper.findById(accepted.id());
        assertThat(stored.status()).isEqualTo(JobStatus.SUCCEEDED.name());
        assertThat(stored.stage()).isEqualTo(JobStage.DONE.name());

        // 查询接口只读：反复查询不会改变状态
        jobService.get(owner, accepted.id());
        jobService.get(owner, accepted.id());
        assertThat(jobMapper.findById(accepted.id()).status()).isEqualTo(JobStatus.SUCCEEDED.name());
    }

    @Test
    @DisplayName("素材数量与模板要求不一致时拒绝受理")
    void rejectWhenAssetCountMismatch() {
        Asset only = newAsset("only.png");

        assertThatThrownBy(() -> jobService.create(owner, "travel-postcard", List.of(only.id())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("需要 2 张图片");
    }

    @Test
    @DisplayName("不能使用别人的素材，也不能读别人的任务")
    void rejectCrossUserAccess() {
        Asset first = newAsset("first.png");
        Asset second = newAsset("second.png");
        JobService.JobResponse accepted = jobService.create(owner, "travel-postcard",
                List.of(first.id(), second.id()));

        AppUser intruder = authService.register("intruder", "闯入者", "intruder123");

        assertThatThrownBy(() -> jobService.create(intruder, "travel-postcard",
                List.of(first.id(), second.id())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("其他账号的素材");

        assertThatThrownBy(() -> jobService.get(intruder, accepted.id()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("任务不存在");
    }

    private Asset newAsset(String name) {
        String relative = "uploads/test/" + UUID.randomUUID() + ".png";
        Path path = storageService.resolve(relative);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, PNG);
        } catch (Exception e) {
            throw new IllegalStateException("测试素材写入失败", e);
        }
        Asset asset = new Asset(
                UUID.randomUUID().toString(),
                owner.id(),
                name,
                "image/png",
                PNG.length,
                relative,
                LocalDateTime.now());
        assetMapper.insert(asset);
        return asset;
    }
}
