package com.guiderag.chat.config;

import com.guiderag.chat.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（rag-chat 模块）
 *
 * 作用：
 * 1. 注册用户上下文拦截器
 * 2. 配置拦截路径和白名单
 *
 * 拦截策略：
 * - 拦截所有请求（/**）
 * - 排除 Knife4j 文档路径（无需用户上下文）
 *
 * @author Guide-RAG Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // 用户上下文拦截器
    private final UserContextInterceptor userContextInterceptor;

    /**
     * 注册拦截器
     *
     * 执行顺序：
     * 1. preHandle  → 提取用户 ID 存入 ThreadLocal
     * 2. Controller → 业务逻辑（可通过 UserContextHolder.getUserId() 获取）
     * 3. afterCompletion → 清理 ThreadLocal
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")

                // 排除 Knife4j 文档路径（这些请求不需要用户上下文）
                .excludePathPatterns(
                        "/doc.html",              // Knife4j UI 页面
                        "/webjars/**",            // 前端资源（JS/CSS）
                        "/v3/api-docs/**",        // OpenAPI 文档 JSON
                        "/swagger-resources/**"   // Swagger 资源
                );

        log.info("✅ 用户上下文拦截器已注册");
    }
}