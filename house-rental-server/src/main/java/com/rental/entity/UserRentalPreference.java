package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_rental_preference")
public class UserRentalPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private String preferredCity;

    private String preferredArea;

    private String preferredHouseType;

    private String workplace;

    private Integer maxCommuteMinutes;

    private String preferenceTags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
