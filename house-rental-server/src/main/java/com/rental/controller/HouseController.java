package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.HouseAddDTO;
import com.rental.dto.HouseStatusUpdateDTO;
import com.rental.dto.HouseUpdateDTO;
import com.rental.entity.House;
import com.rental.exception.BusinessException;
import com.rental.service.HouseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @PostMapping("/landlord/house/add")
    public Result<String> add(@RequestBody @Valid HouseAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        dto.setPublisherId(RequestUserUtil.getCurrentUserId(request));
        houseService.add(dto);
        return Result.success("房源发布成功");
    }

    @PutMapping("/landlord/house/update/{id}")
    public Result<String> update(@PathVariable Long id,
                                 @RequestBody @Valid HouseUpdateDTO dto,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        houseService.update(id, RequestUserUtil.getCurrentUserId(request), dto);
        return Result.success("房源更新成功");
    }

    @PostMapping("/landlord/house/status/{id}")
    public Result<String> updateStatus(@PathVariable Long id,
                                       @RequestBody @Valid HouseStatusUpdateDTO dto,
                                       HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        houseService.updateStatus(id, RequestUserUtil.getCurrentUserId(request), dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "房源已上架" : "房源已下架");
    }

    @GetMapping("/house/list")
    public Result<List<House>> list() {
        return Result.success(houseService.list());
    }

    @GetMapping("/house/detail/{id}")
    public Result<House> detail(@PathVariable Long id) {
        return Result.success(houseService.publicDetail(id));
    }

    @GetMapping("/landlord/house/detail/{id}")
    public Result<House> landlordDetail(@PathVariable Long id,
                                        HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        return Result.success(houseService.publisherDetail(id, RequestUserUtil.getCurrentUserId(request)));
    }

    @GetMapping("/admin/house/detail/{id}")
    public Result<House> adminDetail(@PathVariable Long id,
                                     HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(houseService.adminDetail(id));
    }

    @GetMapping("/admin/house/audit/list")
    public Result<List<House>> auditList(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(houseService.auditList());
    }

    @PostMapping({"/admin/house/audit/pass/{id}", "/admin/house/audit/approve/{id}"})
    public Result<String> auditPass(@PathVariable Long id,
                                    HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        houseService.auditPass(id);
        return Result.success("审核通过");
    }

    @PostMapping("/admin/house/audit/reject/{id}")
    public Result<String> auditReject(@PathVariable Long id,
                                      HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        houseService.auditReject(id);
        return Result.success("审核拒绝");
    }

    @GetMapping("/landlord/house/my/{publisherId}")
    public Result<List<House>> myList(@PathVariable Long publisherId,
                                      HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        if (!currentUserId.equals(publisherId)) {
            throw new BusinessException("只能查看自己的房源列表");
        }
        return Result.success(houseService.myList(currentUserId));
    }

    @DeleteMapping("/landlord/house/delete/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        Long currentUserId = RequestUserUtil.getCurrentUserId(request);
        House house = houseService.detail(id);
        if (!currentUserId.equals(house.getPublisherId())) {
            throw new BusinessException("只能删除自己的房源");
        }
        houseService.delete(id);
        return Result.success("删除成功");
    }
}
