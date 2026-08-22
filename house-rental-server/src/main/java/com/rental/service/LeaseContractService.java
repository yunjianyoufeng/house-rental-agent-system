package com.rental.service;

import com.rental.dto.LeaseContractCreateDTO;
import com.rental.entity.LeaseContract;

import java.util.List;

public interface LeaseContractService {

    void create(LeaseContractCreateDTO dto);

    List<LeaseContract> tenantList(Long tenantId);

    List<LeaseContract> landlordList(Long landlordId);

    List<LeaseContract> adminList();

    LeaseContract detail(Long id);

    void updateContractUrl(Long id, Long operatorId, boolean admin, String contractUrl);

    void finish(Long id);
}
