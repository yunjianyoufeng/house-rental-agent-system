package com.rental.dto;

import lombok.Data;

@Data
public class EmbeddingRecommendHouseDTO {

    /**
     * 房源ID
     */
    private Long id;

    /**
     * 房源文本
     */
    private String text;
}