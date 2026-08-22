package com.rental.dto;

import lombok.Data;

@Data
public class EmbeddingRecommendResponseDTO {

    /**
     * Python 服务返回的房源ID
     */
    private Long houseId;

    /**
     * Python 服务返回的语义相似度分数，通常在 0 到 1 之间
     */
    private Double score;
}