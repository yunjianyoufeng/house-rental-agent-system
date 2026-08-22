package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.NoticeAddDTO;
import com.rental.entity.Notice;
import com.rental.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping("/admin/notice/add")
    public Result<String> add(@RequestBody @Valid NoticeAddDTO dto,
                              HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        noticeService.add(dto);
        return Result.success("公告发布成功");
    }

    @PutMapping("/admin/notice/update/{id}")
    public Result<String> update(@PathVariable Long id,
                                 @RequestBody @Valid NoticeAddDTO dto,
                                 HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        noticeService.update(id, dto);
        return Result.success("公告更新成功");
    }

    @GetMapping("/notice/list")
    public Result<List<Notice>> list() {
        return Result.success(noticeService.list());
    }

    @GetMapping("/notice/detail/{id}")
    public Result<Notice> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }

    @DeleteMapping("/admin/notice/delete/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        noticeService.delete(id);
        return Result.success("公告删除成功");
    }
}
