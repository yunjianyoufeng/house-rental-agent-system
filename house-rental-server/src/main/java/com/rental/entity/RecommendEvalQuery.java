package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommend_eval_query")
public class RecommendEvalQuery {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 测试查询语句
     */
    private String queryText;

    /**
     * 场景类型，例如：预算、交通、学生、装修、家庭
     */
    private String sceneType;

    private LocalDateTime createTime;
}