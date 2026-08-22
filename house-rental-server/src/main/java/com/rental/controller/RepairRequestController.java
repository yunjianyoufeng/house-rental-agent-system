package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.RepairAddDTO;
import com.rental.dto.RepairProcessDTO;
import com.rental.entity.RepairRequest;
import com.rental.exception.BusinessException;
import com.rental.service.RepairRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RepairRequestController {

    private final RepairRequestService repairRequestService;

    public RepairRequestController(RepairRequestService repairRequestService) {
        this.repairRequestService = repairRequestService;
    }

    @PostMapping("/tenant/repair/add")
    public Result<String> add(@RequestBody @Valid RepairAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setTenantId(RequestUserUtil.getCurrentUserId(request));
        repairRequestService.add(dto);
        return Result.success("报修提交成功");
    }

    @GetMapping("/tenant/repair/list/{tenantId}")
    public Result<List<RepairRequest>> tenantList(@PathVariable Long tenantId,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(tenantId)) {
            throw new BusinessException("只能查看自己的报修记录");
        }
        return Result.success(repairRequestService.tenantList(tenantId));
    }

    @GetMapping("/landlord/repair/list/{landlordId}")
    public Result<List<RepairRequest>> landlordList(@PathVariable Long landlordId,
                                                    HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(landlordId)) {
            throw new BusinessException("只能查看自己的报修列表");
        }
        return Result.success(repairRequestService.landlordList(landlordId));
    }

    @GetMapping("/admin/repair/list")
    public Result<List<RepairRequest>> adminList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(repairRequestService.adminList());
    }

    @PostMapping("/landlord/repair/process/{id}")
    public Result<String> process(@PathVariable Long id,
                                  @RequestBody @Valid RepairProcessDTO dto,
                                  HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean exists = repairRequestService.landlordList(currentUserId)
                .stream()
                .anyMatch(item -> id.equals(item.getId()));
        if (!exists) {
            throw new BusinessException("只能处理自己房源的报修记录");
        }
        repairRequestService.process(id, dto);
        return Result.success("处理成功");
    }
}
