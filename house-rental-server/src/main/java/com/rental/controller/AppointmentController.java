package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.AppointmentAddDTO;
import com.rental.entity.Appointment;
import com.rental.exception.BusinessException;
import com.rental.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/tenant/appointment/add")
    public Result<String> add(@RequestBody @Valid AppointmentAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setTenantId(RequestUserUtil.getCurrentUserId(request));
        appointmentService.add(dto);
        return Result.success("预约成功");
    }

    @GetMapping("/tenant/appointment/list/{tenantId}")
    public Result<List<Appointment>> tenantList(@PathVariable Long tenantId,
                                                HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(tenantId)) {
            throw new BusinessException("只能查看自己的预约记录");
        }
        return Result.success(appointmentService.tenantList(tenantId));
    }

    @GetMapping("/landlord/appointment/list/{landlordId}")
    public Result<List<Appointment>> landlordList(@PathVariable Long landlordId,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(landlordId)) {
            throw new BusinessException("只能查看自己的预约列表");
        }
        return Result.success(appointmentService.landlordList(landlordId));
    }

    @PostMapping("/landlord/appointment/approve/{id}")
    public Result<String> approve(@PathVariable Long id,
                                  HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean exists = appointmentService.landlordList(currentUserId)
                .stream()
                .anyMatch(item -> id.equals(item.getId()));
        if (!exists) {
            throw new BusinessException("只能处理自己房源的预约记录");
        }
        appointmentService.approve(id);
        return Result.success("已同意预约");
    }

    @PostMapping("/landlord/appointment/reject/{id}")
    public Result<String> reject(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        boolean exists = appointmentService.landlordList(currentUserId)
                .stream()
                .anyMatch(item -> id.equals(item.getId()));
        if (!exists) {
            throw new BusinessException("只能处理自己房源的预约记录");
        }
        appointmentService.reject(id);
        return Result.success("已拒绝预约");
    }
}
