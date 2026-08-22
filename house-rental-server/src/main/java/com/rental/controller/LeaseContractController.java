package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.ContractFileUpdateDTO;
import com.rental.dto.LeaseContractCreateDTO;
import com.rental.entity.LeaseContract;
import com.rental.exception.BusinessException;
import com.rental.service.LeaseContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LeaseContractController {

    private final LeaseContractService leaseContractService;

    public LeaseContractController(LeaseContractService leaseContractService) {
        this.leaseContractService = leaseContractService;
    }

    @PostMapping("/landlord/contract/create")
    public Result<String> create(@RequestBody @Valid LeaseContractCreateDTO dto,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        dto.setLandlordId(RequestUserUtil.getCurrentUserId(request));
        leaseContractService.create(dto);
        return Result.success("合同创建成功");
    }


    @PutMapping("/landlord/contract/file/{id}")
    public Result<String> updateContractFile(@PathVariable Long id,
                                             @RequestBody @Valid ContractFileUpdateDTO dto,
                                             HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        leaseContractService.updateContractUrl(id, RequestUserUtil.getCurrentUserId(request), false, dto.getContractUrl());
        return Result.success("合同附件更新成功");
    }

    @PutMapping("/admin/contract/file/{id}")
    public Result<String> adminUpdateContractFile(@PathVariable Long id,
                                                  @RequestBody @Valid ContractFileUpdateDTO dto,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        leaseContractService.updateContractUrl(id, RequestUserUtil.getCurrentUserId(request), true, dto.getContractUrl());
        return Result.success("合同附件更新成功");
    }

    @PostMapping("/landlord/contract/finish/{id}")
    public Result<String> finish(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        LeaseContract contract = leaseContractService.detail(id);
        if (!RequestUserUtil.getCurrentUserId(request).equals(contract.getLandlordId())) {
            throw new BusinessException("只能结束自己的合同");
        }
        leaseContractService.finish(id);
        return Result.success("合同已结束");
    }

    @PostMapping("/admin/contract/finish/{id}")
    public Result<String> adminFinish(@PathVariable Long id,
                                      HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        leaseContractService.finish(id);
        return Result.success("合同已结束");
    }

    @GetMapping("/tenant/contract/list/{tenantId}")
    public Result<List<LeaseContract>> tenantList(@PathVariable Long tenantId,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(tenantId)) {
            throw new BusinessException("只能查看自己的合同列表");
        }
        return Result.success(leaseContractService.tenantList(tenantId));
    }

    @GetMapping("/landlord/contract/list/{landlordId}")
    public Result<List<LeaseContract>> landlordList(@PathVariable Long landlordId,
                                                    HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(landlordId)) {
            throw new BusinessException("只能查看自己的合同列表");
        }
        return Result.success(leaseContractService.landlordList(landlordId));
    }

    @GetMapping("/admin/contract/list")
    public Result<List<LeaseContract>> adminList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(leaseContractService.adminList());
    }

    @GetMapping("/contract/detail/{id}")
    public Result<LeaseContract> detail(@PathVariable Long id,
                                        HttpServletRequest request) {
        LeaseContract contract = leaseContractService.detail(id);
        String roleCode = RequestUserUtil.getCurrentRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);

        if ("TENANT".equals(roleCode) && !currentUserId.equals(contract.getTenantId())) {
            throw new BusinessException("只能查看自己的合同详情");
        }
        if ("LANDLORD".equals(roleCode) && !currentUserId.equals(contract.getLandlordId())) {
            throw new BusinessException("只能查看自己相关的合同详情");
        }
        return Result.success(contract);
    }
}
