package com.guiderag.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus 向量数据库配置
 *
 * 核心功能：
 * 1. 从 application-dev.yml 读取 Milvus 连接参数
 * 2. 创建 MilvusServiceClient 单例 Bean
 * 3. 配置连接池、超时时间、重试策略
 *
 * 配置项说明：
 * - host: Milvus 服务地址（默认 localhost）
 * - port: Milvus 服务端口（默认 19530）
 * - database: 数据库名称（多租户隔离，默认 default）
 * - connectTimeoutMs: 连接超时时间（毫秒）
 * - keepAliveTimeMs: 心跳间隔（毫秒）
 * - idleTimeoutMs: 空闲连接超时（毫秒）
 * @author Guide-RAG Team
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    /**
     * Milvus 服务地址
     *
     * 环境配置示例：
     * - 本地开发：localhost 或 127.0.0.1
     * - Docker：milvus-standalone（容器名）
     * - 生产环境：milvus.example.com
     */
    private String host = "localhost";

    /**
     * Milvus 服务端口
     *
     * 默认端口：
     * - gRPC 端口：19530（客户端连接）
     * - HTTP 端口：9091（健康检查）
     * - Metrics 端口：9092（监控指标）
     */
    private Integer port = 19530;

    /**
     * 数据库名称
     *
     * 多租户隔离策略：
     * 1. 数据库级隔离（推荐）：每个租户一个独立数据库
     *    - 优点：数据物理隔离、权限控制简单
     *    - 缺点：数据库数量有上限（默认 64 个）
     */
    private String database = "default";

    /**
     * 用户名（Milvus 2.2+ 支持）
     *
     * 生产环境建议：
     * - 不使用 root 账号
     * - 为应用创建专用账号，授予最小权限
     * - 使用 Kubernetes Secret 管理密码
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     *
     */
    private Long connectTimeoutMs = 10000L;

    /**
     * 心跳间隔（毫秒）
     *
     * 作用：
     * - 保持连接活跃，避免被防火墙关闭
     * - 及时检测连接断开
     */
    private Long keepAliveTimeMs = 55000L;

    /**
     * 空闲连接超时（毫秒）
     *
     * 作用：
     * - 自动关闭长时间未使用的连接
     * - 释放服务器资源
     */
    private Long idleTimeoutMs = 300000L;

    /**
     * 是否使用 TLS 加密
     *
     * 生产环境必须开启：
     * - 防止中间人攻击
     * - 符合安全合规要求
     * - 需要配置证书（server.pem, client.pem）
     */
    private Boolean secure = false;

    /**
     * 创建 MilvusServiceClient Bean
     *
     * 生命周期：
     * 1. Spring 容器启动时创建（单例）
     * 2. 应用运行期间复用同一实例
     * 3. Spring 容器关闭时自动断开连接
     *
     * 线程安全：
     * - MilvusServiceClient 内部使用 gRPC Channel 池
     * - 多线程并发调用安全
     * - 不需要额外的同步控制
     *
     * 异常处理：
     * - 连接失败会抛出 RuntimeException
     * - 建议配置健康检查端点（Actuator）
     * - 使用重试机制提高可用性
     *
     * @return MilvusServiceClient 实例
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("[MilvusConfig] 初始化 Milvus 客户端: host={}, port={}, database={}",
                host, port, database);

        // 构建连接参数
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .withKeepAliveTime(keepAliveTimeMs, TimeUnit.MILLISECONDS)
                .withIdleTimeout(idleTimeoutMs, TimeUnit.MILLISECONDS);

        // 认证配置
        if (username != null && !username.isEmpty()) {
            log.info("[MilvusConfig] 启用认证: username={}", username);
            builder.withAuthorization(username, password);
        }

        // 配置TLS
        if (Boolean.TRUE.equals(secure)) {
            log.info("[MilvusConfig] 启用 TLS 加密");
            builder.withSecure(true);
        }

        // 创建客户端
        MilvusServiceClient client = new MilvusServiceClient(builder.build());

        log.info("[MilvusConfig] Milvus 客户端初始化成功");
        return client;
    }
}
