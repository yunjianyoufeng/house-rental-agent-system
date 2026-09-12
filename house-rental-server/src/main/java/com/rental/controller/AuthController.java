package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.LoginDTO;
import com.rental.dto.RegisterDTO;
import com.rental.entity.SysUser;
import com.rental.common.RequestUserUtil;
import com.rental.common.SessionTokenUtil;
import com.rental.service.RequestLimitService;
import com.rental.service.TokenService;
import com.rental.service.UserService;
import com.rental.vo.AuthenticatedUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final RequestLimitService requestLimitService;

    @Value("${app.session.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(UserService userService, TokenService tokenService, RequestLimitService requestLimitService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.requestLimitService = requestLimitService;
    }

    @GetMapping("/csrf")
    public Result<String> csrf(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        // 显式读取惰性令牌，让 Spring 将 XSRF-TOKEN Cookie 写入响应。
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        token.getToken();
        return Result.success("CSRF 令牌已就绪");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid RegisterDTO dto, HttpServletRequest request) {
        requestLimitService.check("register-ip", request.getRemoteAddr(), 5, Duration.ofHours(1));
        userService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody @Valid LoginDTO dto,
                                 HttpServletRequest request, HttpServletResponse response) {
        // 不信任公网可伪造的 X-Forwarded-For；反向代理限流另外使用真实连接来源。
        requestLimitService.check("login-ip", request.getRemoteAddr(), 30, Duration.ofMinutes(15));
        requestLimitService.check("login-account", dto.getUsername().strip().toLowerCase(Locale.ROOT),
                10, Duration.ofMinutes(15));
        SysUser user = userService.login(dto);

        tokenService.deleteToken(SessionTokenUtil.extract(request));
        String token = tokenService.createToken(user);
        writeSessionCookie(response, token, TokenService.ABSOLUTE_EXPIRE);
        user.setToken(null);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        return Result.success("登录成功", user);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = SessionTokenUtil.extract(request);

        if (token != null) {
            tokenService.deleteToken(token);
        }
        writeSessionCookie(response, "", Duration.ZERO);

        return Result.success("退出登录成功");
    }

    @GetMapping("/me")
    public Result<AuthenticatedUserVO> me(HttpServletRequest request) {
        SysUser user = RequestUserUtil.getCurrentUser(request);
        return Result.success(new AuthenticatedUserVO(
                user.getId(),
                user.getRoleCode(),
                user.getStatus()
        ));
    }

    private void writeSessionCookie(HttpServletResponse response, String token, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SessionTokenUtil.COOKIE_NAME, token)
                .httpOnly(true).secure(cookieSecure).sameSite("Strict").path("/").maxAge(maxAge).build().toString());
    }
}
