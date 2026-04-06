package com.guiderag.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性映射类
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 服务地址 (例如: http://127.0.0.1:9000)
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥内容
     */
    private String secretKey;

    /**
     * 存储桶名称 (默认为 guiderag)
     */
    private String bucketName = "guiderag";
}
