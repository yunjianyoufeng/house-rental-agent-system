package com.rental.service;

import com.rental.dto.LeaseOrderCreateDTO;
import com.rental.dto.LeaseOrderPayDTO;
import com.rental.entity.LeaseOrder;
import com.rental.vo.PaymentStartVO;

import java.util.List;

public interface LeaseOrderService {

    void create(LeaseOrderCreateDTO dto);

    List<LeaseOrder> tenantList(Long tenantId);

    List<LeaseOrder> landlordList(Long landlordId);

    List<LeaseOrder> adminList();

    PaymentStartVO startPayment(Long id, LeaseOrderPayDTO dto);

    void pay(Long id, LeaseOrderPayDTO dto);

    void cancel(Long id, Long tenantId);

    LeaseOrder detail(Long id);
}
