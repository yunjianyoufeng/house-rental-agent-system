package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommend_record")
public class RecommendRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，当前推荐接口是公开接口，所以第一版可以为空
     */
    private Long userId;

    /**
     * 用户输入的租房需求
     */
    private String queryText;

    /**
     * 推荐模型类型，例如 RULE、TFIDF、EMBEDDING
     */
    private String modelType;

    /**
     * 推荐结果中的房源ID，使用英文逗号分隔
     */
    private String resultHouseIds;

    /**
     * 响应时间，单位毫秒
     */
    private Integer responseTimeMs;

    private LocalDateTime createTime;
}