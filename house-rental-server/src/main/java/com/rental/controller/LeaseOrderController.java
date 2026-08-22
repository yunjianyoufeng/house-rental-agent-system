package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.LeaseOrderCreateDTO;
import com.rental.dto.LeaseOrderPayDTO;
import com.rental.entity.LeaseOrder;
import com.rental.exception.BusinessException;
import com.rental.service.LeaseOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LeaseOrderController {

    private final LeaseOrderService leaseOrderService;

    public LeaseOrderController(LeaseOrderService leaseOrderService) {
        this.leaseOrderService = leaseOrderService;
    }

    @PostMapping("/tenant/order/create")
    public Result<String> create(@RequestBody @Valid LeaseOrderCreateDTO dto,
                                 HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setTenantId(RequestUserUtil.getCurrentUserId(request));
        leaseOrderService.create(dto);
        return Result.success("订单创建成功");
    }

    @GetMapping("/tenant/order/list/{tenantId}")
    public Result<List<LeaseOrder>> tenantList(@PathVariable Long tenantId,
                                               HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(tenantId)) {
            throw new BusinessException("只能查看自己的订单");
        }
        return Result.success(leaseOrderService.tenantList(tenantId));
    }

    @GetMapping("/landlord/order/list/{landlordId}")
    public Result<List<LeaseOrder>> landlordList(@PathVariable Long landlordId,
                                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(landlordId)) {
            throw new BusinessException("只能查看自己的订单列表");
        }
        return Result.success(leaseOrderService.landlordList(landlordId));
    }

    @GetMapping("/admin/order/list")
    public Result<List<LeaseOrder>> adminList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(leaseOrderService.adminList());
    }

    @PostMapping("/tenant/order/pay/start/{id}")
    public Result<com.rental.vo.PaymentStartVO> startPay(@PathVariable Long id,
                                                         @RequestBody @Valid LeaseOrderPayDTO dto,
                                                         HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        LeaseOrder order = leaseOrderService.detail(id);
        if (!currentUserId.equals(order.getTenantId())) {
            throw new BusinessException("只能支付自己的订单");
        }
        return Result.success(leaseOrderService.startPayment(id, dto));
    }

    @PostMapping("/tenant/order/pay/{id}")
    public Result<String> pay(@PathVariable Long id,
                              @RequestBody @Valid LeaseOrderPayDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        LeaseOrder order = leaseOrderService.detail(id);
        if (!currentUserId.equals(order.getTenantId())) {
            throw new BusinessException("只能支付自己的订单");
        }
        leaseOrderService.pay(id, dto);
        return Result.success("支付成功");
    }

    @PostMapping("/tenant/order/cancel/{id}")
    public Result<String> cancel(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        leaseOrderService.cancel(id, RequestUserUtil.getCurrentUserId(request));
        return Result.success("订单已取消");
    }

    @GetMapping("/order/detail/{id}")
    public Result<LeaseOrder> detail(@PathVariable Long id,
                                     HttpServletRequest request) {
        LeaseOrder order = leaseOrderService.detail(id);
        String roleCode = RequestUserUtil.getCurrentRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);

        if ("TENANT".equals(roleCode) && !currentUserId.equals(order.getTenantId())) {
            throw new BusinessException("只能查看自己的订单详情");
        }
        if ("LANDLORD".equals(roleCode)) {
            boolean exists = leaseOrderService.landlordList(currentUserId)
                    .stream()
                    .anyMatch(item -> id.equals(item.getId()));
            if (!exists) {
                throw new BusinessException("只能查看自己相关的订单详情");
            }
        }
        return Result.success(order);
    }
}
