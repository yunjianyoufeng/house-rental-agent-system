package com.rental.service;

import com.rental.dto.RecommendEvalDTO;
import com.rental.vo.RecommendEvalSummaryVO;

public interface RecommendEvaluationService {

    RecommendEvalSummaryVO evaluate(RecommendEvalDTO dto);
}