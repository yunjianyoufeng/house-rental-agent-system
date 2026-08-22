package com.rental.common;

import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

public class RequestUserUtil {

    private RequestUserUtil() {
    }

    public static Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserId");
        if (attr instanceof Long userId) {
            return userId;
        }
        if (attr instanceof Integer userId) {
            return userId.longValue();
        }
        throw new BusinessException("未获取到当前登录用户信息");
    }

    public static String getCurrentRole(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserRole");
        if (attr != null) {
            return String.valueOf(attr);
        }
        throw new BusinessException("未获取到当前登录角色信息");
    }

    public static SysUser getCurrentUser(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUser");
        if (attr instanceof SysUser user) {
            return user;
        }
        throw new BusinessException("未获取到当前登录用户信息");
    }

    public static void checkAdminRole(HttpServletRequest request) {
        String roleCode = getCurrentRole(request);
        if (!"ADMIN".equals(roleCode)) {
            throw new BusinessException("无权限访问管理员接口");
        }
    }

    public static void checkTenantRole(HttpServletRequest request) {
        String roleCode = getCurrentRole(request);
        if (!"TENANT".equals(roleCode)) {
            throw new BusinessException("无权限访问租客接口");
        }
    }

    public static void checkLandlordRole(HttpServletRequest request) {
        String roleCode = getCurrentRole(request);
        if (!"LANDLORD".equals(roleCode)) {
            throw new BusinessException("无权限访问出租者接口");
        }
    }
}
