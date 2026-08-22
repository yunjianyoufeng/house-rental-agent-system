package com.rental.service;

import com.rental.dto.RentalApplicationAddDTO;
import com.rental.entity.RentalApplication;

import java.util.List;

public interface RentalApplicationService {

    void add(RentalApplicationAddDTO dto);

    List<RentalApplication> tenantList(Long tenantId);

    List<RentalApplication> landlordList(Long landlordId);

    List<RentalApplication> adminList();

    void approve(Long id);

    void reject(Long id);
}