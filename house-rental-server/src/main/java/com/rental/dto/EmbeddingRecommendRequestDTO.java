package com.rental.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingRecommendRequestDTO {

    /**
     * 用户自然语言租房需求
     */
    private String query;

    /**
     * 推荐数量
     */
    private Integer topK;

    /**
     * 候选房源列表
     */
    private List<EmbeddingRecommendHouseDTO> houses;
}