package com.xiaoying.studio.simulation;

import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.job.JobStage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发/验收用的模拟控制接口。不属于正式业务页面，README 会说明用法。
 *
 * <pre>
 * POST /api/dev/simulation  {"failStage":"GENERATE","failCount":1,"stageDelayMs":4000}
 * POST /api/dev/simulation/reset
 * GET  /api/dev/simulation
 * </pre>
 */
@RestController
@RequestMapping("/api/dev/simulation")
public class SimulationController {

    private final SimulationState simulationState;

    public SimulationController(SimulationState simulationState) {
        this.simulationState = simulationState;
    }

    public record UpdateRequest(String failStage, Integer failCount, Long stageDelayMs) {
    }

    @GetMapping
    public SimulationState.Snapshot current() {
        return simulationState.snapshot();
    }

    @PostMapping
    public SimulationState.Snapshot update(@RequestBody UpdateRequest request) {
        if (request.failStage() != null) {
            JobStage stage = JobStage.fromString(request.failStage());
            boolean allowed = stage == JobStage.MATERIAL_CHECK || stage == JobStage.GENERATE
                    || stage == JobStage.FINALIZE || stage == JobStage.QUEUED;
            if (!allowed) {
                throw ApiException.badRequest("INVALID_STAGE",
                        "failStage 只能是 MATERIAL_CHECK / GENERATE / FINALIZE / QUEUED");
            }
            simulationState.apply(stage.name(), request.failCount() == null ? 1 : request.failCount());
        } else if (request.failCount() != null) {
            simulationState.apply(simulationState.snapshot().failStage(), request.failCount());
        }
        if (request.stageDelayMs() != null) {
            simulationState.setStageDelayMs(request.stageDelayMs());
        }
        return simulationState.snapshot();
    }

    @PostMapping("/reset")
    public SimulationState.Snapshot reset() {
        simulationState.reset();
        return simulationState.snapshot();
    }
}
