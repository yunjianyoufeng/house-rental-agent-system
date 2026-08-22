package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseRecommendDTO {

    @NotBlank(message = "请输入租房需求")
    private String query;

    /**
     * 城市，可选
     */
    private String city;

    /**
     * 区域，可选
     */
    private String area;

    /**
     * 最高租金，可选
     */
    private BigDecimal maxRent;

    /**
     * 最小面积，可选
     */
    private BigDecimal minSquare;

    /**
     * 户型，可选
     */
    private String houseType;


    /**
     * 是否使用大语言模型解析需求。
     * 普通推荐默认使用，模型评价时关闭，避免大量调用 DeepSeek。
     */
    private Boolean useLlmParse = true;

    /**
     * 推荐模型类型：
     * RULE：规则推荐
     * 后续可以扩展 TFIDF、EMBEDDING
     */
    private String modelType = "RULE";

    /**
     * 返回推荐数量
     */
    private Integer topK = 5;

}