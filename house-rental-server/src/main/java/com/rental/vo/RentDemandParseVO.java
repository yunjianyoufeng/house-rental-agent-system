package com.rental.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RentDemandParseVO {

    /**
     * 原始输入
     */
    private String query;

    /**
     * 最高租金
     */
    private BigDecimal maxRent;

    /**
     * 最小面积
     */
    private BigDecimal minSquare;

    /**
     * 城市
     */
    private String city;

    /**
     * 区域
     */
    private String area;

    /**
     * 户型
     */
    private String houseType;

    /**
     * 场景标签，例如：学生、交通、装修、学习环境、家庭居住
     */
    private List<String> scenes = new ArrayList<>();

    /**
     * 关键词
     */
    private List<String> keywords = new ArrayList<>();

    /**
     * 是否需要安静环境
     */
    private Boolean needQuiet = false;

    /**
     * 是否需要交通便利
     */
    private Boolean needTraffic = false;

    /**
     * 是否需要精装修/拎包入住
     */
    private Boolean needDecoration = false;

    /**
     * 是否适合学生
     */
    private Boolean studentFriendly = false;

    /**
     * 是否适合家庭居住
     */
    private Boolean familyFriendly = false;

    /**
     * 解析方式：RULE_PARSE，后续可扩展 LLM_PARSE
     */
    private String parseType = "RULE_PARSE";
}