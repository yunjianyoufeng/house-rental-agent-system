package com.rental.vo;

import lombok.Data;

@Data
public class StatisticsOverviewVO {

    private Long userCount;

    private Long houseCount;

    private Long pendingAuditHouseCount;

    private Long orderCount;

    private Long paidOrderCount;

    private Long repairCount;

    private Long complaintCount;

    private Long noticeCount;
}