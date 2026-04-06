package com.guiderag.chat.config;

import com.guiderag.common.constant.AuthConstants;
import com.guiderag.common.context.UserContextHolder;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 配置类
 * 用于拦截通过 Feign 发出的请求，向请求头中注入上下文信息（如当前登录用户的 ID），
 * 以解决跨微服务调用时认证信息丢失导致 downstream 服务拿不到 tenantId 等信息的问题。
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Long userId = UserContextHolder.getUserId();
            if (userId != null) {
                if (log.isDebugEnabled()) {
                    log.debug("FeignRequestInterceptor: 向请求头添加用户信息 {}={}", AuthConstants.USER_ID_HEADER, userId);
                }
                template.header(AuthConstants.USER_ID_HEADER, String.valueOf(userId));
            } else {
                log.warn("FeignRequestInterceptor: 当前上下文中没有用户 ID，请检查是否有拦截器漏配置！路径: {}", template.url());
            }
        };
    }
}
