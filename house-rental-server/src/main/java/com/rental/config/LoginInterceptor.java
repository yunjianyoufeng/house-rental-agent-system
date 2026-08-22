package com.rental.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.common.Result;
import com.rental.entity.SysUser;
import com.rental.service.TokenService;
import com.rental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(TokenService tokenService, UserService userService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        String token = extractToken(authorization);

        if (token == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        Long userId = tokenService.getUserIdByToken(token);
        if (userId == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            writeUnauthorized(response, "用户不存在");
            return false;
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            writeUnauthorized(response, "账号已被禁用");
            return false;
        }

        tokenService.refreshToken(token);
        request.setAttribute("currentUserId", user.getId());
        request.setAttribute("currentUserRole", user.getRoleCode());
        request.setAttribute("currentUser", user);
        return true;
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized(message)));
    }
}
