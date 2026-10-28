package com.xiaoying.studio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 后台任务执行器。
 *
 * <p>刻意不使用 {@code @Async}：任务提交点只有一个（{@code JobDispatcher}），显式注入
 * {@link ThreadPoolTaskExecutor} 更容易在测试里替换成同步执行器，从而让流水线行为可断言。
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "jobExecutor")
    public TaskExecutor jobExecutor(AppProperties properties) {
        AppProperties.Pipeline pipeline = properties.pipeline();
        if (pipeline.synchronous()) {
            // 测试用：任务在调用线程里直接执行，流水线行为可以被断言
            return new SyncTaskExecutor();
        }
        int workers = Math.max(1, pipeline.workerThreads());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(Math.max(1, pipeline.queueCapacity()));
        executor.setThreadNamePrefix("job-worker-");
        // 停机时把在跑的任务做完，避免任务永远停在 PROCESSING
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
