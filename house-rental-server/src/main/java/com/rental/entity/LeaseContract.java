package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("lease_contract")
public class LeaseContract {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long houseId;

    private Long tenantId;

    private Long landlordId;

    private Long applicationId;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal monthlyRent;

    private BigDecimal deposit;

    private String contractUrl;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String houseTitle;

    @TableField(exist = false)
    private String tenantName;

    @TableField(exist = false)
    private String landlordName;
}
