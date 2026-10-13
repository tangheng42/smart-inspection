package com.zhixunyun.context;


/**
 * 保存当前 HTTP 请求线程中的身份信息，供服务层做数据权限判断。
 * 由 JWT 拦截器写入，并在请求结束时移除；不会自动传递到异步线程。
 */
public class BaseContext {

    private static final ThreadLocal<Long> CURRENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        CURRENT_ID.set(id);
    }

    public static Long getCurrentId() {
        return CURRENT_ID.get();
    }

    public static void removeCurrentId() {
        CURRENT_ID.remove();
    }

    public static void setCurrentRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getCurrentRole() {
        return CURRENT_ROLE.get();
    }

    public static void removeCurrentRole() {
        CURRENT_ROLE.remove();
    }

    public static void remove() {
        CURRENT_ID.remove();
        CURRENT_ROLE.remove();
    }
}
