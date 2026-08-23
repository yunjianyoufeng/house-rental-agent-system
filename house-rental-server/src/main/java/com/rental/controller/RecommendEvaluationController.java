package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.RecommendEvalDTO;
import com.rental.service.RecommendEvaluationService;
import com.rental.vo.RecommendEvalSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend")
public class RecommendEvaluationController {

    private final RecommendEvaluationService recommendEvaluationService;

    public RecommendEvaluationController(RecommendEvaluationService recommendEvaluationService) {
        this.recommendEvaluationService = recommendEvaluationService;
    }

    @PostMapping("/evaluate")
    public Result<RecommendEvalSummaryVO> evaluate(@RequestBody RecommendEvalDTO dto,
                                                    HttpServletRequest request) {
        RequestUserUtil.checkAdminRole(request);
        return Result.success(recommendEvaluationService.evaluate(dto));
    }
}
