package com.rental.service.impl;

import com.rental.config.PaymentProperties;
import com.rental.dto.LeaseOrderPayDTO;
import com.rental.entity.House;
import com.rental.entity.LeaseContract;
import com.rental.entity.LeaseOrder;
import com.rental.exception.BusinessException;
import com.rental.mapper.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaseOrderPaymentSecurityTest {
    private final LeaseOrderMapper orders = mock(LeaseOrderMapper.class);
    private final LeaseContractMapper contracts = mock(LeaseContractMapper.class);
    private final HouseMapper houses = mock(HouseMapper.class);
    private final PaymentProperties properties = new PaymentProperties();
    private final LeaseOrderServiceImpl service = new LeaseOrderServiceImpl(
            orders, contracts, houses, mock(SysUserMapper.class), properties);

    @Test
    void productionCannotStartOrCompleteSimulatedPayment() {
        LeaseOrderPayDTO dto = new LeaseOrderPayDTO();
        dto.setPayType("ALIPAY");
        assertThrows(BusinessException.class, () -> service.startPayment(1L, dto));
        assertThrows(BusinessException.class, () -> service.pay(1L, dto));
        verifyNoInteractions(orders, contracts, houses);
    }

    @Test
    void explicitlyEnabledDemoStillCompletesPayment() {
        properties.setDemoMode(true);
        LeaseOrder order = new LeaseOrder();
        order.setId(1L);
        order.setContractId(2L);
        order.setPayStatus(0);
        LeaseContract contract = new LeaseContract();
        contract.setHouseId(3L);
        contract.setStatus(0);
        House house = new House();
        house.setStatus(2);
        when(orders.selectById(1L)).thenReturn(order);
        when(contracts.selectById(2L)).thenReturn(contract);
        when(houses.selectById(3L)).thenReturn(house);
        when(orders.updateById(order)).thenReturn(1);
        when(contracts.updateById(contract)).thenReturn(1);
        when(houses.updateById(house)).thenReturn(1);
        LeaseOrderPayDTO dto = new LeaseOrderPayDTO();
        dto.setPayType("ALIPAY");
        service.pay(1L, dto);
        assertEquals(1, order.getPayStatus());
        assertEquals(1, contract.getStatus());
        assertEquals(3, house.getStatus());
    }
}
