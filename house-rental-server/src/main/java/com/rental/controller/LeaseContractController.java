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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@RestController
public class LeaseContractController {

    private static final Pattern CONTRACT_URL_PATTERN = Pattern.compile(
            "^/uploads/contracts/\\d{8}/[a-fA-F0-9]{32}\\.(pdf|doc|docx|jpg|jpeg|png)$"
    );

    private final LeaseContractService leaseContractService;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

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
        checkContractAccess(contract, request);
        return Result.success(contract);
    }

    @GetMapping("/contract/file/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                  HttpServletRequest request) {
        LeaseContract contract = leaseContractService.detail(id);
        checkContractAccess(contract, request);

        String contractUrl = contract.getContractUrl();
        if (contractUrl == null || !CONTRACT_URL_PATTERN.matcher(contractUrl).matches()) {
            throw new BusinessException("合同附件地址非法或附件不存在");
        }

        Path uploadRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path filePath = uploadRoot.resolve(contractUrl.substring("/uploads/".length())).normalize();
        Path contractRoot = uploadRoot.resolve("contracts").normalize();
        if (!filePath.startsWith(contractRoot) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
            throw new BusinessException("合同附件不存在");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (Exception exception) {
            throw new BusinessException("合同附件读取失败");
        }
    }

    private void checkContractAccess(LeaseContract contract, HttpServletRequest request) {
        String roleCode = RequestUserUtil.getCurrentRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean allowed = "ADMIN".equals(roleCode)
                || ("TENANT".equals(roleCode) && currentUserId.equals(contract.getTenantId()))
                || ("LANDLORD".equals(roleCode) && currentUserId.equals(contract.getLandlordId()));
        if (!allowed) {
            throw new BusinessException("无权查看该合同附件");
        }
    }
}
