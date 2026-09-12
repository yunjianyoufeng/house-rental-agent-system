package com.rental.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.common.Result;
import com.rental.common.SessionTokenUtil;
import com.rental.entity.SysUser;
import com.rental.service.TokenService;
import com.rental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
        String token = SessionTokenUtil.extract(request);

        // 推荐允许游客访问；携带凭证时仍校验身份，不能信任前端传来的角色。
        if (token == null && "/recommend/house".equals(request.getServletPath())) {
            return true;
        }

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

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SessionTokenUtil.COOKIE_NAME, "")
                .path("/").httpOnly(true).sameSite("Strict").maxAge(0).build().toString());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized(message)));
    }
}
