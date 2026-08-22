package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.RecommendEvalDTO;
import com.rental.service.RecommendEvaluationService;
import com.rental.vo.RecommendEvalSummaryVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend")
public class RecommendEvaluationController {

    private final RecommendEvaluationService recommendEvaluationService;

    public RecommendEvaluationController(RecommendEvaluationService recommendEvaluationService) {
        this.recommendEvaluationService = recommendEvaluationService;
    }

    @PostMapping("/evaluate")
    public Result<RecommendEvalSummaryVO> evaluate(@RequestBody RecommendEvalDTO dto) {
        return Result.success(recommendEvaluationService.evaluate(dto));
    }
}