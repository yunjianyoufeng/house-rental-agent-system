package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lease_order")
public class LeaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private Long tenantId;

    private BigDecimal amount;

    private String payType;

    private Integer payStatus;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Long houseId;

    @TableField(exist = false)
    private String houseTitle;

    @TableField(exist = false)
    private String tenantName;

    @TableField(exist = false)
    private String landlordName;
}
