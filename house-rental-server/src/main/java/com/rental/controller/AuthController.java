package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.LoginDTO;
import com.rental.dto.RegisterDTO;
import com.rental.entity.SysUser;
import com.rental.service.TokenService;
import com.rental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody @Valid LoginDTO dto) {
        SysUser user = userService.login(dto);

        String token = tokenService.createToken(user);
        user.setToken(token);

        return Result.success("登录成功", user);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String token = extractToken(authorization);

        if (token != null) {
            tokenService.deleteToken(token);
        }

        return Result.success("退出登录成功");
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
}