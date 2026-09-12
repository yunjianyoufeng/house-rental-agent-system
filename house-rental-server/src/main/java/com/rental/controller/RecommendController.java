package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.HouseRecommendDTO;
import com.rental.service.RecommendService;
import com.rental.service.RequestLimitService;
import jakarta.servlet.http.HttpServletRequest;
import com.rental.vo.HouseRecommendVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Duration;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    private final RecommendService recommendService;
    private final RequestLimitService requestLimitService;

    public RecommendController(RecommendService recommendService, RequestLimitService requestLimitService) {
        this.recommendService = recommendService;
        this.requestLimitService = requestLimitService;
    }

    @PostMapping("/house")
    public Result<List<HouseRecommendVO>> recommendHouse(@RequestBody @Valid HouseRecommendDTO dto,
                                                        HttpServletRequest request) {
        requestLimitService.check("recommend-ip", request.getRemoteAddr(), 30, Duration.ofMinutes(1));
        boolean tenant = "TENANT".equals(request.getAttribute("currentUserRole"));
        // 匿名用户不能通过 useLlmParse/modelType 绕过付费解析和计算额度。
        dto.setUseLlmParse(tenant && !Boolean.FALSE.equals(dto.getUseLlmParse()));
        if (!tenant) {
            dto.setModelType("RULE");
        }
        if (Boolean.TRUE.equals(dto.getUseLlmParse())) {
            requestLimitService.check("llm-user", String.valueOf(request.getAttribute("currentUserId")),
                    20, Duration.ofHours(1));
        }
        return Result.success(recommendService.recommendHouse(dto));
    }
}
