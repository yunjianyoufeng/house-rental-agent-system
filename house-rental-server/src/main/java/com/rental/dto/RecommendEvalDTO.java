package com.rental.dto;

import lombok.Data;

@Data
public class RecommendEvalDTO {

    /**
     * 推荐模型类型：
     * RULE：规则推荐
     * 后续可以扩展 TFIDF、EMBEDDING
     */
    private String modelType = "RULE";

    /**
     * TopK 评价，例如 Precision@5
     */
    private Integer topK = 5;

    /**
     * 场景类型，可选。
     * 例如：预算学生、交通、装修、学习环境、家庭居住
     */
    private String sceneType;
}