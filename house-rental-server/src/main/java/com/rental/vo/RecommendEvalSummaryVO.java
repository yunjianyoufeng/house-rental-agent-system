package com.rental.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendEvalSummaryVO {

    private String modelType;

    private Integer topK;

    private Integer queryCount;

    private Double avgPrecision;

    private Double avgRecall;

    private Double avgF1;

    private Double avgNdcg;

    private Double avgResponseTimeMs;

    private List<RecommendEvalDetailVO> details;
}