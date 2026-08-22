package com.rental.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendEvalDetailVO {

    private Long queryId;

    private String queryText;

    private String sceneType;

    /**
     * 模型推荐出来的房源ID
     */
    private List<Long> recommendedHouseIds;

    /**
     * 人工标注的相关房源ID
     */
    private List<Long> relevantHouseIds;

    /**
     * 命中的相关房源数量
     */
    private Integer hitCount;

    /**
     * 人工标注相关房源数量
     */
    private Integer relevantCount;

    private Double precision;

    private Double recall;

    private Double f1;

    private Double ndcg;

    private Long responseTimeMs;
}