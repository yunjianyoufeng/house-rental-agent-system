package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rental.dto.RentalApplicationAddDTO;
import com.rental.entity.House;
import com.rental.entity.LeaseContract;
import com.rental.entity.LeaseOrder;
import com.rental.entity.RentalApplication;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.LeaseContractMapper;
import com.rental.mapper.LeaseOrderMapper;
import com.rental.mapper.RentalApplicationMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.RentalApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class RentalApplicationServiceImpl implements RentalApplicationService {

    private final RentalApplicationMapper rentalApplicationMapper;
    private final HouseMapper houseMapper;
    private final LeaseContractMapper leaseContractMapper;
    private final LeaseOrderMapper leaseOrderMapper;
    private final SysUserMapper sysUserMapper;

    public RentalApplicationServiceImpl(RentalApplicationMapper rentalApplicationMapper,
                                        HouseMapper houseMapper,
                                        LeaseContractMapper leaseContractMapper,
                                        LeaseOrderMapper leaseOrderMapper,
                                        SysUserMapper sysUserMapper) {
        this.rentalApplicationMapper = rentalApplicationMapper;
        this.houseMapper = houseMapper;
        this.leaseContractMapper = leaseContractMapper;
        this.leaseOrderMapper = leaseOrderMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void add(RentalApplicationAddDTO dto) {
        House house = houseMapper.selectById(dto.getHouseId());
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (house.getAuditStatus() == null || house.getAuditStatus() != 1) {
            throw new BusinessException("房源未通过审核，暂不可申请");
        }
        if (house.getStatus() == null || house.getStatus() != 1) {
            throw new BusinessException("该房源当前不可申请");
        }
        if (dto.getTenantId().equals(house.getPublisherId())) {
            throw new BusinessException("不能申请自己发布的房源");
        }

        LambdaQueryWrapper<RentalApplication> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(RentalApplication::getHouseId, dto.getHouseId())
                .eq(RentalApplication::getTenantId, dto.getTenantId())
                .in(RentalApplication::getStatus, 0, 1);
        if (rentalApplicationMapper.selectCount(duplicateWrapper) > 0) {
            throw new BusinessException("您已提交过该房源申请，请勿重复操作");
        }

        RentalApplication application = new RentalApplication();
        application.setHouseId(dto.getHouseId());
        application.setTenantId(dto.getTenantId());
        application.setLandlordId(house.getPublisherId());
        application.setRemark(dto.getRemark());
        application.setStatus(0);

        int rows = rentalApplicationMapper.insert(application);
        if (rows <= 0) {
            throw new BusinessException("提交申请失败");
        }
    }

    @Override
    public List<RentalApplication> tenantList(Long tenantId) {
        LambdaQueryWrapper<RentalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalApplication::getTenantId, tenantId)
                .orderByDesc(RentalApplication::getId);
        List<RentalApplication> list = rentalApplicationMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<RentalApplication> landlordList(Long landlordId) {
        LambdaQueryWrapper<RentalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalApplication::getLandlordId, landlordId)
                .orderByDesc(RentalApplication::getId);
        List<RentalApplication> list = rentalApplicationMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<RentalApplication> adminList() {
        LambdaQueryWrapper<RentalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(RentalApplication::getId);
        List<RentalApplication> list = rentalApplicationMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        RentalApplication application = rentalApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("租房申请不存在");
        }
        if (application.getStatus() != null && application.getStatus() != 0) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }

        House house = houseMapper.selectById(application.getHouseId());
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (house.getStatus() == null || house.getStatus() != 1) {
            throw new BusinessException("该房源当前不可审批，可能已被锁定或出租");
        }

        int claimedRows = houseMapper.update(
                null,
                new LambdaUpdateWrapper<House>()
                        .eq(House::getId, house.getId())
                        .eq(House::getStatus, 1)
                        .set(House::getStatus, 2)
        );
        if (claimedRows <= 0) {
            throw new BusinessException("该房源已被其他申请锁定，请刷新后重试");
        }

        LambdaQueryWrapper<RentalApplication> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(RentalApplication::getHouseId, application.getHouseId())
                .eq(RentalApplication::getStatus, 1)
                .ne(RentalApplication::getId, application.getId());
        if (rentalApplicationMapper.selectCount(approvedWrapper) > 0) {
            throw new BusinessException("该房源已有已通过的申请，不能重复审批");
        }

        LambdaQueryWrapper<LeaseContract> contractExistsWrapper = new LambdaQueryWrapper<>();
        contractExistsWrapper.eq(LeaseContract::getApplicationId, application.getId());
        if (leaseContractMapper.selectCount(contractExistsWrapper) > 0) {
            throw new BusinessException("该申请已生成合同");
        }

        application.setStatus(1);
        rentalApplicationMapper.updateById(application);

        LeaseContract contract = new LeaseContract();
        contract.setHouseId(application.getHouseId());
        contract.setTenantId(application.getTenantId());
        contract.setLandlordId(application.getLandlordId());
        contract.setApplicationId(application.getId());
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusYears(1).minusDays(1));
        contract.setMonthlyRent(house.getRentPrice());
        contract.setDeposit(house.getDeposit());
        contract.setStatus(0);
        int contractRows = leaseContractMapper.insert(contract);
        if (contractRows <= 0) {
            throw new BusinessException("自动生成合同失败");
        }

        LeaseOrder order = new LeaseOrder();
        order.setContractId(contract.getId());
        order.setTenantId(application.getTenantId());
        order.setAmount(house.getRentPrice().add(house.getDeposit()));
        order.setPayStatus(0);
        int orderRows = leaseOrderMapper.insert(order);
        if (orderRows <= 0) {
            throw new BusinessException("自动生成订单失败");
        }

        LambdaQueryWrapper<RentalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalApplication::getHouseId, application.getHouseId())
                .ne(RentalApplication::getId, application.getId())
                .eq(RentalApplication::getStatus, 0);
        List<RentalApplication> applicationList = rentalApplicationMapper.selectList(wrapper);
        for (RentalApplication item : applicationList) {
            item.setStatus(2);
            rentalApplicationMapper.updateById(item);
        }
    }

    @Override
    public void reject(Long id) {
        RentalApplication application = rentalApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("租房申请不存在");
        }
        if (application.getStatus() != null && application.getStatus() != 0) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }
        application.setStatus(2);
        rentalApplicationMapper.updateById(application);
    }

    private void fillDisplayFields(List<RentalApplication> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (RentalApplication application : list) {
            fillDisplayFields(application);
        }
    }

    private void fillDisplayFields(RentalApplication application) {
        if (application == null) {
            return;
        }
        House house = houseMapper.selectById(application.getHouseId());
        application.setHouseTitle(house != null ? house.getTitle() : "--");
        application.setTenantName(userDisplayName(application.getTenantId()));
        application.setLandlordName(userDisplayName(application.getLandlordId()));
    }

    private String userDisplayName(Long userId) {
        if (userId == null) {
            return "--";
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "用户" + userId;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户" + userId;
    }
}
