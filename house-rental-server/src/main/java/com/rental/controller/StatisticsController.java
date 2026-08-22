package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.service.StatisticsService;
import com.rental.vo.StatisticsOverviewVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/admin/statistics/overview")
    public Result<StatisticsOverviewVO> overview(HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(statisticsService.overview());
    }
}