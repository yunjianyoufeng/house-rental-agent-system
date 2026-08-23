package com.rental.controller;

import com.rental.exception.BusinessException;
import com.rental.service.AppointmentService;
import com.rental.service.ComplaintService;
import com.rental.service.LeaseContractService;
import com.rental.service.LeaseOrderService;
import com.rental.service.RentalApplicationService;
import com.rental.service.RepairRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentPersonalToolControllerTest {

    private AppointmentService appointmentService;
    private LeaseContractService leaseContractService;
    private AgentPersonalToolController controller;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        RentalApplicationService rentalApplicationService = mock(RentalApplicationService.class);
        leaseContractService = mock(LeaseContractService.class);
        LeaseOrderService leaseOrderService = mock(LeaseOrderService.class);
        RepairRequestService repairRequestService = mock(RepairRequestService.class);
        ComplaintService complaintService = mock(ComplaintService.class);
        controller = new AgentPersonalToolController(
                appointmentService,
                rentalApplicationService,
                leaseContractService,
                leaseOrderService,
                repairRequestService,
                complaintService
        );
    }

    @Test
    void contractsUsesTrustedCurrentUserId() {
        MockHttpServletRequest request = tenantRequest(8L);
        when(leaseContractService.tenantList(8L)).thenReturn(List.of());

        controller.contracts(request);

        verify(leaseContractService).tenantList(8L);
    }

    @Test
    void rejectsNonTenantBeforeCallingService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("currentUserId", 8L);
        request.setAttribute("currentUserRole", "LANDLORD");

        assertThrows(BusinessException.class, () -> controller.appointments(request));

        verifyNoInteractions(appointmentService);
    }

    private MockHttpServletRequest tenantRequest(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUserRole", "TENANT");
        return request;
    }
}
