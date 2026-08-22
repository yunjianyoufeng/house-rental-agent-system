package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.entity.Complaint;
import com.rental.entity.House;
import com.rental.entity.LeaseOrder;
import com.rental.entity.Notice;
import com.rental.entity.RepairRequest;
import com.rental.entity.SysUser;
import com.rental.mapper.ComplaintMapper;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.LeaseOrderMapper;
import com.rental.mapper.NoticeMapper;
import com.rental.mapper.RepairRequestMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.StatisticsService;
import com.rental.vo.StatisticsOverviewVO;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SysUserMapper sysUserMapper;
    private final HouseMapper houseMapper;
    private final LeaseOrderMapper leaseOrderMapper;
    private final RepairRequestMapper repairRequestMapper;
    private final ComplaintMapper complaintMapper;
    private final NoticeMapper noticeMapper;

    public StatisticsServiceImpl(SysUserMapper sysUserMapper,
                                 HouseMapper houseMapper,
                                 LeaseOrderMapper leaseOrderMapper,
                                 RepairRequestMapper repairRequestMapper,
                                 ComplaintMapper complaintMapper,
                                 NoticeMapper noticeMapper) {
        this.sysUserMapper = sysUserMapper;
        this.houseMapper = houseMapper;
        this.leaseOrderMapper = leaseOrderMapper;
        this.repairRequestMapper = repairRequestMapper;
        this.complaintMapper = complaintMapper;
        this.noticeMapper = noticeMapper;
    }

    @Override
    public StatisticsOverviewVO overview() {
        StatisticsOverviewVO vo = new StatisticsOverviewVO();

        vo.setUserCount(sysUserMapper.selectCount(null));
        vo.setHouseCount(houseMapper.selectCount(null));

        LambdaQueryWrapper<House> pendingHouseWrapper = new LambdaQueryWrapper<>();
        pendingHouseWrapper.eq(House::getAuditStatus, 0);
        vo.setPendingAuditHouseCount(houseMapper.selectCount(pendingHouseWrapper));

        vo.setOrderCount(leaseOrderMapper.selectCount(null));

        LambdaQueryWrapper<LeaseOrder> paidOrderWrapper = new LambdaQueryWrapper<>();
        paidOrderWrapper.eq(LeaseOrder::getPayStatus, 1);
        vo.setPaidOrderCount(leaseOrderMapper.selectCount(paidOrderWrapper));

        vo.setRepairCount(repairRequestMapper.selectCount(null));
        vo.setComplaintCount(complaintMapper.selectCount(null));
        vo.setNoticeCount(noticeMapper.selectCount(null));

        return vo;
    }
}