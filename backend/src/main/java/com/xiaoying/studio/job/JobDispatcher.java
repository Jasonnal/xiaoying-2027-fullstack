package com.xiaoying.studio.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 把任务投递到后台线程池。
 *
 * <p>注入的是 {@link TaskExecutor} 接口而不是具体线程池：测试里换成同步执行器后，
 * 流水线行为就能在测试线程里被完整断言。
 */
@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);

    private final TaskExecutor executor;
    private final JobPipeline pipeline;

    public JobDispatcher(@Qualifier("jobExecutor") TaskExecutor executor, JobPipeline pipeline) {
        this.executor = executor;
        this.pipeline = pipeline;
    }

    public void dispatch(String jobId) {
        executor.execute(() -> {
            try {
                pipeline.run(jobId);
            } catch (Exception ex) {
                // 流水线内部已把可预期失败写库，这里兜底记录线程级异常，避免任务无声消失
                log.error("任务 {} 执行线程异常退出", jobId, ex);
            }
        });
    }
}
