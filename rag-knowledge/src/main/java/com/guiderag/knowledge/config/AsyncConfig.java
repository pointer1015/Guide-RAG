package com.guiderag.knowledge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * <p>
 * 使用 Java 21 虚拟线程特性
 * 虚拟线程特点：
 * - 轻量级：可创建数百万个虚拟线程
 * - I/O 友好：阻塞时自动挂起，不占用平台线程
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 文档处理专用线程池
     * <p>
     * 配置说明：
     * - 核心线程数：4（I/O 密集型任务，可适当提高）
     * - 最大线程数：16（防止过多并发压垮外部服务）
     * - 队列容量：100（缓冲突发请求）
     * - 拒绝策略：CallerRunsPolicy（由调用线程执行，实现限流）
     */
    @Bean("documentProcessExecutor")
    public Executor documentProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数 * 2
        executor.setCorePoolSize(4);

        // 最大线程数
        executor.setMaxPoolSize(16);

        // 队列容量：用于缓冲待处理任务
        executor.setQueueCapacity(100);

        // 线程名前缀：用于日志追踪
        executor.setThreadNamePrefix("doc-process-");

        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最大等待时间
        executor.initialize();

        log.info("文档处理线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 100);

        return executor;
    }
}
