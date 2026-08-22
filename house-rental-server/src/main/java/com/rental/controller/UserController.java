package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.UserStatusUpdateDTO;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    public Result<List<SysUser>> list(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(userService.listAll());
    }

    @PostMapping("/admin/users/status/{id}")
    public Result<String> updateStatus(@PathVariable Long id,
                                       @RequestBody @Valid UserStatusUpdateDTO dto,
                                       HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (currentUserId.equals(id) && Integer.valueOf(0).equals(dto.getStatus())) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        userService.updateStatus(id, dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "用户已启用" : "用户已禁用");
    }
}
