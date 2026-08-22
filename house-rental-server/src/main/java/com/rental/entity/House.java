package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String address;

    private String city;

    private String area;

    private BigDecimal rentPrice;

    private BigDecimal deposit;

    private String houseType;

    private BigDecimal square;

    private String floor;

    private String description;

    private String imageUrls;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer status;

    private Integer auditStatus;

    private Long publisherId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String publisherName;
}
