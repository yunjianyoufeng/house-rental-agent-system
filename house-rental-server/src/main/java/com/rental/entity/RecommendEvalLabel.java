package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommend_eval_label")
public class RecommendEvalLabel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 推荐实验查询ID
     */
    private Long queryId;

    /**
     * 相关房源ID
     */
    private Long houseId;

    /**
     * 相关性评分：1相关，2较相关，3非常相关
     */
    private Integer relevanceScore;

    private LocalDateTime createTime;
}