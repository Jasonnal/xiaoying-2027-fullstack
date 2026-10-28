package com.xiaoying.studio.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用级配置。全部使用 record + 构造绑定，配置项集中在一处，便于 README 对齐。
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Storage storage,
        Pipeline pipeline,
        Simulation simulation,
        Auth auth,
        Cors cors) {

    public record Storage(String root, String publicBasePath) {
    }

    public record Pipeline(long stageDelayMs, int workerThreads, int queueCapacity, boolean synchronous) {
    }

    public record Simulation(String failStage, int failCount) {
    }

    public record Auth(String cookieName, int sessionTtlHours) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
