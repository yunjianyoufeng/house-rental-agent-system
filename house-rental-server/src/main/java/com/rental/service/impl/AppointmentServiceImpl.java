package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.AppointmentAddDTO;
import com.rental.entity.Appointment;
import com.rental.entity.House;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.AppointmentMapper;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final HouseMapper houseMapper;
    private final SysUserMapper sysUserMapper;

    public AppointmentServiceImpl(AppointmentMapper appointmentMapper,
                                  HouseMapper houseMapper,
                                  SysUserMapper sysUserMapper) {
        this.appointmentMapper = appointmentMapper;
        this.houseMapper = houseMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void add(AppointmentAddDTO dto) {
        if (dto.getAppointmentTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("预约时间必须晚于当前时间");
        }

        House house = houseMapper.selectById(dto.getHouseId());
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (house.getAuditStatus() == null || house.getAuditStatus() != 1) {
            throw new BusinessException("房源未通过审核，暂不可预约");
        }
        if (house.getStatus() == null || house.getStatus() != 1) {
            throw new BusinessException("该房源当前不可预约");
        }

        LambdaQueryWrapper<Appointment> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(Appointment::getHouseId, dto.getHouseId())
                .eq(Appointment::getTenantId, dto.getTenantId())
                .eq(Appointment::getStatus, 0);
        if (appointmentMapper.selectCount(duplicateWrapper) > 0) {
            throw new BusinessException("您已有待处理的预约，请勿重复提交");
        }

        Appointment appointment = new Appointment();
        appointment.setHouseId(dto.getHouseId());
        appointment.setTenantId(dto.getTenantId());
        appointment.setLandlordId(house.getPublisherId());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setRemark(dto.getRemark());
        appointment.setStatus(0);

        int rows = appointmentMapper.insert(appointment);
        if (rows <= 0) {
            throw new BusinessException("预约失败");
        }
    }

    @Override
    public List<Appointment> tenantList(Long tenantId) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getTenantId, tenantId)
                .orderByDesc(Appointment::getId);
        List<Appointment> list = appointmentMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<Appointment> landlordList(Long landlordId) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getLandlordId, landlordId)
                .orderByDesc(Appointment::getId);
        List<Appointment> list = appointmentMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public void approve(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (appointment.getStatus() != null && appointment.getStatus() != 0) {
            throw new BusinessException("该预约已处理，请勿重复操作");
        }
        appointment.setStatus(1);
        appointmentMapper.updateById(appointment);
    }

    @Override
    public void reject(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (appointment.getStatus() != null && appointment.getStatus() != 0) {
            throw new BusinessException("该预约已处理，请勿重复操作");
        }
        appointment.setStatus(2);
        appointmentMapper.updateById(appointment);
    }

    private void fillDisplayFields(List<Appointment> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Appointment appointment : list) {
            fillDisplayFields(appointment);
        }
    }

    private void fillDisplayFields(Appointment appointment) {
        if (appointment == null) {
            return;
        }
        House house = houseMapper.selectById(appointment.getHouseId());
        appointment.setHouseTitle(house != null ? house.getTitle() : "--");
        appointment.setTenantName(userDisplayName(appointment.getTenantId()));
        appointment.setLandlordName(userDisplayName(appointment.getLandlordId()));
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
