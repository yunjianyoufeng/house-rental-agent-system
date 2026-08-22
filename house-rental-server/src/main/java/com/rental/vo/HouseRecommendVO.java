package com.rental.vo;

import com.rental.entity.House;
import lombok.Data;

@Data
public class HouseRecommendVO {

    /**
     * 推荐房源
     */
    private House house;

    /**
     * 推荐得分
     */
    private Double score;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 使用的模型类型
     */
    private String modelType;
}