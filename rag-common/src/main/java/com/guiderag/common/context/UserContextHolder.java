package com.guiderag.common.context;


/**
 * 用户上下文持有者，使用ThreadLocal存储当前线程的用户ID
 */
public class UserContextHolder {

    // 使用ThreadLocal存储当前线程的用户ID
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    // 设置用户ID
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    // 获取用户ID
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    // 清除用户ID
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
