package com.rental.service.impl;

import com.rental.dto.AppointmentAddDTO;
import com.rental.entity.Appointment;
import com.rental.mapper.AppointmentMapper;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceIdempotencyTest {

    @Test
    void repeatedRequestKeyDoesNotInsertSecondAppointment() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        HouseMapper houseMapper = mock(HouseMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl(
                appointmentMapper,
                houseMapper,
                userMapper
        );
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withNano(0);
        Appointment existing = new Appointment();
        existing.setHouseId(19L);
        existing.setTenantId(8L);
        existing.setAppointmentTime(appointmentTime);
        existing.setRemark("上午看房");
        existing.setRequestKey("request-key-1");
        when(appointmentMapper.selectOne(any())).thenReturn(existing);

        AppointmentAddDTO dto = new AppointmentAddDTO();
        dto.setHouseId(19L);
        dto.setTenantId(8L);
        dto.setAppointmentTime(appointmentTime);
        dto.setRemark("上午看房");
        dto.setRequestKey("request-key-1");

        service.add(dto);

        verify(appointmentMapper, never()).insert(any(Appointment.class));
        verify(houseMapper, never()).selectById(any());
    }
}
