package com.rental.service;

import com.rental.dto.RepairAddDTO;
import com.rental.dto.RepairProcessDTO;
import com.rental.entity.RepairRequest;

import java.util.List;

public interface RepairRequestService {

    void add(RepairAddDTO dto);

    List<RepairRequest> tenantList(Long tenantId);

    List<RepairRequest> landlordList(Long landlordId);

    List<RepairRequest> adminList();

    void process(Long id, RepairProcessDTO dto);
}
