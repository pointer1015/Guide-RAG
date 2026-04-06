package com.guiderag.chat.interceptor;

import com.guiderag.common.constant.AuthConstants;
import com.guiderag.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 请求前置处理：从请求头提取用户ID并存入 ThreadLocal
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  处理器
     * @return true 继续执行，false 中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 从请求头读取网关透传的用户 ID
        String userIdStr = request.getHeader(AuthConstants.USER_ID_HEADER);

        // 2. 解析并存储到 ThreadLocal（如果存在）
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                UserContextHolder.setUserId(userId);
            } catch (NumberFormatException e) {
                // 记录异常但不中断请求（网关应保证正确性）
                log.warn("无效的用户ID格式: {}", userIdStr);
            }
        }

        return true;
    }

    /**
     * 请求完成后处理（无论成功、异常、响应完成都会执行）
     *
     * 作用：清理 ThreadLocal，防止内存泄漏和数据污染
     *
     * 场景说明：
     * - Tomcat/Undertow 使用线程池处理请求
     * - 如果不清理，下次请求复用该线程时会读到旧的用户 ID
     * - 长期积累会导致内存泄漏（ThreadLocalMap 无法回收）
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  处理器
     * @param ex       异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {

        // 清理当前线程的 ThreadLocal，防止内存泄漏
        UserContextHolder.clear();

        // 可选：记录调试日志（生产环境建议关闭）
        if (log.isDebugEnabled()) {
            log.debug("已清理 ThreadLocal 用户上下文，请求路径: {}", request.getRequestURI());
        }
    }
}
