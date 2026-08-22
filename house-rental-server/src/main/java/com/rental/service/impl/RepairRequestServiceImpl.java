package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.RepairAddDTO;
import com.rental.dto.RepairProcessDTO;
import com.rental.entity.House;
import com.rental.entity.LeaseContract;
import com.rental.entity.RepairRequest;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.LeaseContractMapper;
import com.rental.mapper.RepairRequestMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.RepairRequestService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class RepairRequestServiceImpl implements RepairRequestService {

    private final RepairRequestMapper repairRequestMapper;
    private final HouseMapper houseMapper;
    private final LeaseContractMapper leaseContractMapper;
    private final SysUserMapper sysUserMapper;

    public RepairRequestServiceImpl(RepairRequestMapper repairRequestMapper,
                                    HouseMapper houseMapper,
                                    LeaseContractMapper leaseContractMapper,
                                    SysUserMapper sysUserMapper) {
        this.repairRequestMapper = repairRequestMapper;
        this.houseMapper = houseMapper;
        this.leaseContractMapper = leaseContractMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void add(RepairAddDTO dto) {
        House house = houseMapper.selectById(dto.getHouseId());
        if (house == null) {
            throw new BusinessException("房源不存在");
        }

        LambdaQueryWrapper<LeaseContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(LeaseContract::getHouseId, dto.getHouseId())
                .eq(LeaseContract::getTenantId, dto.getTenantId())
                .eq(LeaseContract::getStatus, 1)
                .orderByDesc(LeaseContract::getId)
                .last("limit 1");
        LeaseContract contract = leaseContractMapper.selectOne(contractWrapper);
        if (contract == null) {
            throw new BusinessException("只有签约中的租客才能提交该房源报修");
        }

        LocalDate today = LocalDate.now();
        if (contract.getStartDate() == null || contract.getEndDate() == null
                || today.isBefore(contract.getStartDate()) || today.isAfter(contract.getEndDate())) {
            throw new BusinessException("当前合同不在有效期内，暂不能提交报修");
        }

        RepairRequest repair = new RepairRequest();
        repair.setHouseId(dto.getHouseId());
        repair.setTenantId(dto.getTenantId());
        repair.setLandlordId(contract.getLandlordId());
        repair.setContent(dto.getContent());
        repair.setStatus(0);

        int rows = repairRequestMapper.insert(repair);
        if (rows <= 0) {
            throw new BusinessException("报修提交失败");
        }
    }

    @Override
    public List<RepairRequest> tenantList(Long tenantId) {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairRequest::getTenantId, tenantId)
                .orderByDesc(RepairRequest::getId);
        List<RepairRequest> list = repairRequestMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<RepairRequest> landlordList(Long landlordId) {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairRequest::getLandlordId, landlordId)
                .orderByDesc(RepairRequest::getId);
        List<RepairRequest> list = repairRequestMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<RepairRequest> adminList() {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(RepairRequest::getId);
        List<RepairRequest> list = repairRequestMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public void process(Long id, RepairProcessDTO dto) {
        RepairRequest repair = repairRequestMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("报修记录不存在");
        }
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
            throw new BusinessException("报修状态只允许更新为处理中或已完成");
        }
        repair.setStatus(dto.getStatus());
        repair.setResult(dto.getResult());
        repairRequestMapper.updateById(repair);
    }

    private void fillDisplayFields(List<RepairRequest> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (RepairRequest repair : list) {
            fillDisplayFields(repair);
        }
    }

    private void fillDisplayFields(RepairRequest repair) {
        if (repair == null) {
            return;
        }
        House house = houseMapper.selectById(repair.getHouseId());
        repair.setHouseTitle(house != null ? house.getTitle() : "--");
        repair.setTenantName(userDisplayName(repair.getTenantId()));
        repair.setLandlordName(userDisplayName(repair.getLandlordId()));
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
