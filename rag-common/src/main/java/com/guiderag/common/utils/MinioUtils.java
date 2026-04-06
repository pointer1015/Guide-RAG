package com.guiderag.common.utils;

import com.guiderag.common.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件操作工具类
 */
@Slf4j
@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioConfig;

    /**
     * 上传文件
     *
     * @param file 响应前端传来的 MultipartFile
     * @return 文件的存储对象名称 (ObjectName)
     */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;

        try {
            // 确保 Bucket 存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
            }

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            return objectName;
        } catch (Exception e) {
            log.error("MinIO 上传失败: ", e);
            throw new RuntimeException("文件上传失败，请稍后重试");
        }
    }

    /**
     * 获取文件的临时访问 URL (默认过期时间为 7 天)
     *
     * @param objectName 存储在 MinIO 中的对象名称
     * @return 完整的访问 URL
     */
    public String getPreviewUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预览链接失败: ", e);
            return null;
        }
    }
}
