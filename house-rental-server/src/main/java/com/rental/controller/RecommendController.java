package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.HouseRecommendDTO;
import com.rental.service.RecommendService;
import com.rental.vo.HouseRecommendVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/house")
    public Result<List<HouseRecommendVO>> recommendHouse(@RequestBody @Valid HouseRecommendDTO dto,
                                                          HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(recommendService.recommendHouse(dto));
    }
}
