package com.rental.service;

import com.rental.dto.AppointmentAddDTO;
import com.rental.entity.Appointment;

import java.util.List;

public interface AppointmentService {

    void add(AppointmentAddDTO dto);

    List<Appointment> tenantList(Long tenantId);

    List<Appointment> landlordList(Long landlordId);

    void approve(Long id);

    void reject(Long id);
}