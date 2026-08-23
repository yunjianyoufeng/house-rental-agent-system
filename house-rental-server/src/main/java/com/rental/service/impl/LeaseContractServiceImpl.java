package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.LeaseContractCreateDTO;
import com.rental.entity.House;
import com.rental.entity.LeaseContract;
import com.rental.entity.RentalApplication;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.LeaseContractMapper;
import com.rental.mapper.RentalApplicationMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.LeaseContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class LeaseContractServiceImpl implements LeaseContractService {

    private static final Pattern CONTRACT_URL_PATTERN = Pattern.compile(
            "^/uploads/contracts/\\d{8}/[a-fA-F0-9]{32}\\.(pdf|doc|docx|jpg|jpeg|png)$"
    );

    private final LeaseContractMapper leaseContractMapper;
    private final RentalApplicationMapper rentalApplicationMapper;
    private final HouseMapper houseMapper;
    private final SysUserMapper sysUserMapper;

    public LeaseContractServiceImpl(LeaseContractMapper leaseContractMapper,
                                    RentalApplicationMapper rentalApplicationMapper,
                                    HouseMapper houseMapper,
                                    SysUserMapper sysUserMapper) {
        this.leaseContractMapper = leaseContractMapper;
        this.rentalApplicationMapper = rentalApplicationMapper;
        this.houseMapper = houseMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void create(LeaseContractCreateDTO dto) {
        RentalApplication application = rentalApplicationMapper.selectById(dto.getApplicationId());
        if (application == null) {
            throw new BusinessException("租房申请不存在");
        }
        if (application.getStatus() == null || application.getStatus() != 1) {
            throw new BusinessException("只有已同意的申请才能创建合同");
        }
        if (!dto.getLandlordId().equals(application.getLandlordId())) {
            throw new BusinessException("只能为自己的申请创建合同");
        }
        if (!dto.getTenantId().equals(application.getTenantId()) || !dto.getHouseId().equals(application.getHouseId())) {
            throw new BusinessException("合同信息与申请记录不一致");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate()) || dto.getEndDate().isEqual(dto.getStartDate())) {
            throw new BusinessException("合同结束日期必须晚于开始日期");
        }

        LambdaQueryWrapper<LeaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseContract::getApplicationId, dto.getApplicationId());
        if (leaseContractMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该申请已存在合同，请勿重复创建");
        }

        LeaseContract contract = new LeaseContract();
        contract.setHouseId(dto.getHouseId());
        contract.setTenantId(dto.getTenantId());
        contract.setLandlordId(dto.getLandlordId());
        contract.setApplicationId(dto.getApplicationId());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setMonthlyRent(dto.getMonthlyRent());
        contract.setDeposit(dto.getDeposit());
        contract.setContractUrl(StringUtils.hasText(dto.getContractUrl())
                ? validateContractUrl(dto.getContractUrl())
                : null);
        contract.setStatus(0);

        int rows = leaseContractMapper.insert(contract);
        if (rows <= 0) {
            throw new BusinessException("创建合同失败");
        }
    }

    @Override
    public List<LeaseContract> tenantList(Long tenantId) {
        LambdaQueryWrapper<LeaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseContract::getTenantId, tenantId)
                .orderByDesc(LeaseContract::getId);
        List<LeaseContract> list = leaseContractMapper.selectList(wrapper);
        refreshContracts(list);
        return list;
    }

    @Override
    public List<LeaseContract> landlordList(Long landlordId) {
        LambdaQueryWrapper<LeaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseContract::getLandlordId, landlordId)
                .orderByDesc(LeaseContract::getId);
        List<LeaseContract> list = leaseContractMapper.selectList(wrapper);
        refreshContracts(list);
        return list;
    }

    @Override
    public List<LeaseContract> adminList() {
        LambdaQueryWrapper<LeaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LeaseContract::getId);
        List<LeaseContract> list = leaseContractMapper.selectList(wrapper);
        refreshContracts(list);
        return list;
    }

    @Override
    public LeaseContract detail(Long id) {
        LeaseContract contract = leaseContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        refreshContract(contract);
        return contract;
    }


    @Override
    public void updateContractUrl(Long id, Long operatorId, boolean admin, String contractUrl) {
        LeaseContract contract = detail(id);
        if (!admin && !operatorId.equals(contract.getLandlordId())) {
            throw new BusinessException("只能上传自己合同的附件");
        }
        contract.setContractUrl(validateContractUrl(contractUrl));
        if (leaseContractMapper.updateById(contract) <= 0) {
            throw new BusinessException("更新合同附件失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long id) {
        LeaseContract contract = leaseContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        refreshContract(contract);
        if (contract.getStatus() == null || contract.getStatus() != 1) {
            throw new BusinessException("只有生效中的合同才能结束");
        }
        contract.setStatus(2);
        if (leaseContractMapper.updateById(contract) <= 0) {
            throw new BusinessException("合同结束失败");
        }

        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null && Integer.valueOf(3).equals(house.getStatus())) {
            house.setStatus(0);
            houseMapper.updateById(house);
        }
    }

    private void refreshContracts(List<LeaseContract> list) {
        if (list == null) {
            return;
        }
        for (LeaseContract item : list) {
            refreshContract(item);
        }
    }

    private String validateContractUrl(String contractUrl) {
        String normalized = contractUrl == null ? "" : contractUrl.trim();
        if (!CONTRACT_URL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("合同附件地址非法，请先通过合同上传接口上传文件");
        }
        return normalized;
    }

    private void refreshContract(LeaseContract contract) {
        if (contract == null) {
            return;
        }
        if (contract.getStatus() != null && contract.getEndDate() != null
                && contract.getStatus() == 1 && contract.getEndDate().isBefore(LocalDate.now())) {
            contract.setStatus(2);
            leaseContractMapper.updateById(contract);
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null && Integer.valueOf(3).equals(house.getStatus())) {
                house.setStatus(0);
                houseMapper.updateById(house);
            }
        }
        fillDisplayFields(contract);
    }

    private void fillDisplayFields(LeaseContract contract) {
        if (contract == null) {
            return;
        }
        House house = houseMapper.selectById(contract.getHouseId());
        contract.setHouseTitle(house != null ? house.getTitle() : "--");
        contract.setTenantName(userDisplayName(contract.getTenantId()));
        contract.setLandlordName(userDisplayName(contract.getLandlordId()));
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
