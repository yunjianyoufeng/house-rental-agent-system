package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.RentalApplicationAddDTO;
import com.rental.entity.RentalApplication;
import com.rental.exception.BusinessException;
import com.rental.service.RentalApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RentalApplicationController {

    private final RentalApplicationService rentalApplicationService;

    public RentalApplicationController(RentalApplicationService rentalApplicationService) {
        this.rentalApplicationService = rentalApplicationService;
    }

    @PostMapping("/tenant/application/add")
    public Result<String> add(@RequestBody @Valid RentalApplicationAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setTenantId(RequestUserUtil.getCurrentUserId(request));
        rentalApplicationService.add(dto);
        return Result.success("申请提交成功");
    }

    @GetMapping("/tenant/application/list/{tenantId}")
    public Result<List<RentalApplication>> tenantList(@PathVariable Long tenantId,
                                                      HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(tenantId)) {
            throw new BusinessException("只能查看自己的租房申请");
        }
        return Result.success(rentalApplicationService.tenantList(tenantId));
    }

    @GetMapping("/landlord/application/list/{landlordId}")
    public Result<List<RentalApplication>> landlordList(@PathVariable Long landlordId,
                                                        HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(landlordId)) {
            throw new BusinessException("只能查看自己的租房申请列表");
        }
        return Result.success(rentalApplicationService.landlordList(landlordId));
    }

    @PostMapping("/landlord/application/approve/{id}")
    public Result<String> approve(@PathVariable Long id,
                                  HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean exists = rentalApplicationService.landlordList(currentUserId)
                .stream()
                .anyMatch(item -> id.equals(item.getId()));
        if (!exists) {
            throw new BusinessException("只能处理自己房源的租房申请");
        }
        rentalApplicationService.approve(id);
        return Result.success("审核通过");
    }

    @PostMapping("/landlord/application/reject/{id}")
    public Result<String> reject(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean exists = rentalApplicationService.landlordList(currentUserId)
                .stream()
                .anyMatch(item -> id.equals(item.getId()));
        if (!exists) {
            throw new BusinessException("只能处理自己房源的租房申请");
        }
        rentalApplicationService.reject(id);
        return Result.success("已拒绝");
    }

    @GetMapping("/admin/application/list")
    public Result<List<RentalApplication>> adminList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(rentalApplicationService.adminList());
    }
}
