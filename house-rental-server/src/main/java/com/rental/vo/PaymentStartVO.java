package com.rental.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentStartVO {

    private Long orderId;

    private BigDecimal amount;

    private String payType;

    private String payTypeLabel;

    private Boolean demoMode;

    private String instruction;

    private String qrCodeBase64;

    private String cashierTitle;

    private String cashierDescription;
}
