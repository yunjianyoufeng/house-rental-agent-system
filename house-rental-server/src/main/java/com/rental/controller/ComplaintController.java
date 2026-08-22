package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.ComplaintAddDTO;
import com.rental.dto.ComplaintProcessDTO;
import com.rental.entity.Complaint;
import com.rental.exception.BusinessException;
import com.rental.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/tenant/complaint/add")
    public Result<String> add(@RequestBody @Valid ComplaintAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setUserId(RequestUserUtil.getCurrentUserId(request));
        complaintService.add(dto);
        return Result.success("投诉提交成功");
    }

    @GetMapping("/tenant/complaint/list/{userId}")
    public Result<List<Complaint>> userList(@PathVariable Long userId,
                                            HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(userId)) {
            throw new BusinessException("只能查看自己的投诉记录");
        }
        return Result.success(complaintService.userList(userId));
    }

    @GetMapping("/admin/complaint/list")
    public Result<List<Complaint>> adminList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(complaintService.adminList());
    }

    @PostMapping("/admin/complaint/process/{id}")
    public Result<String> process(@PathVariable Long id,
                                  @RequestBody @Valid ComplaintProcessDTO dto,
                                  HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        complaintService.process(id, dto);
        return Result.success("投诉处理成功");
    }
}
