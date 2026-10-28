package com.xiaoying.studio.job;

import com.xiaoying.studio.identity.AppUser;
import com.xiaoying.studio.identity.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    public record CreateJobRequest(
            @NotBlank(message = "不能为空") String templateId,
            @NotEmpty(message = "至少需要一张素材") List<String> assetIds) {
    }

    /** 受理后立刻返回 201 + 任务快照，不等待制作结束。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobService.JobResponse create(@Valid @RequestBody CreateJobRequest request,
                                         @CurrentUser AppUser user) {
        return jobService.create(user, request.templateId(), request.assetIds());
    }

    @GetMapping("/{id}")
    public JobService.JobResponse detail(@PathVariable String id, @CurrentUser AppUser user) {
        return jobService.get(user, id);
    }

    /** 个人历史记录（当前账号），按创建时间倒序。 */
    @GetMapping
    public List<JobService.JobResponse> list(@RequestParam(defaultValue = "30") int limit,
                                             @CurrentUser AppUser user) {
        return jobService.list(user, limit);
    }
}
