package com.guiderag.knowledge.interceptor;

import com.guiderag.common.constant.AuthConstants;
import com.guiderag.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 作用：从网关透传的请求头 X-User-Id 中读取用户 ID，存入当前线程的 ThreadLocal
 * 注意：此拦截器不做任何鉴权逻辑，鉴权由上游网关（rag-gateway）统一负责
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.从请求头中读取用户ID
        String userIdStr = request.getHeader(AuthConstants.USER_ID_HEADER);

        // 2.将userIdStr转换为Long并存入ThreadLocal
        if (userIdStr != null && !userIdStr.isEmpty()) {
            Long userId = Long.parseLong(userIdStr);
            UserContextHolder.setUserId(userId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完成后，清理ThreadLocal中的用户ID，防止内存泄漏
        UserContextHolder.clear();
    }
}
