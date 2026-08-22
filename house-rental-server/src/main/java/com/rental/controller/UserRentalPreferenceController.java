package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.UserRentalPreferenceUpdateDTO;
import com.rental.entity.UserRentalPreference;
import com.rental.service.UserRentalPreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/agent/preferences")
public class UserRentalPreferenceController {

    private final UserRentalPreferenceService preferenceService;

    public UserRentalPreferenceController(UserRentalPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public Result<UserRentalPreference> get(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long userId = RequestUserUtil.getCurrentUserId(request);
        return Result.success(preferenceService.getByUserId(userId));
    }

    @PutMapping
    public Result<UserRentalPreference> save(
            @RequestBody @Valid UserRentalPreferenceUpdateDTO dto,
            HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long userId = RequestUserUtil.getCurrentUserId(request);
        return Result.success(preferenceService.saveOrUpdate(userId, dto));
    }

    @DeleteMapping
    public Result<String> remove(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long userId = RequestUserUtil.getCurrentUserId(request);
        preferenceService.removeByUserId(userId);
        return Result.success("已清除长期租房偏好");
    }
}
