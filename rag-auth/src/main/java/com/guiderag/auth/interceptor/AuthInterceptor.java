package com.guiderag.auth.interceptor;

import com.guiderag.common.exception.AuthException;
import com.guiderag.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.guiderag.common.context.UserContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        // 1.检查Token是否存在，是否符合规范
        if (token != null && token.startsWith("Bearer ")) {

            token = token.substring(7); // 去掉 "Bearer " 前缀

        }

        // 2.检查redis拦截黑名单
        String blacklistKey = "auth:blacklist:token:" + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new AuthException("Token已注销或失效，请重新登录");
        }

        // 3. 解析 Token 获取 userId 并注入当前线程上下文
        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new AuthException("Token验证失败或已过期");
        }

        // 将 userId 注入当前线程上下文
        UserContextHolder.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成，必须清理上下文
        UserContextHolder.clear();
    }

}
