package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.entity.Appointment;
import com.rental.entity.Complaint;
import com.rental.entity.LeaseContract;
import com.rental.entity.LeaseOrder;
import com.rental.entity.RentalApplication;
import com.rental.entity.RepairRequest;
import com.rental.service.AppointmentService;
import com.rental.service.ComplaintService;
import com.rental.service.LeaseContractService;
import com.rental.service.LeaseOrderService;
import com.rental.service.RentalApplicationService;
import com.rental.service.RepairRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenant/agent-tools/me")
public class AgentPersonalToolController {

    private final AppointmentService appointmentService;
    private final RentalApplicationService rentalApplicationService;
    private final LeaseContractService leaseContractService;
    private final LeaseOrderService leaseOrderService;
    private final RepairRequestService repairRequestService;
    private final ComplaintService complaintService;

    public AgentPersonalToolController(
            AppointmentService appointmentService,
            RentalApplicationService rentalApplicationService,
            LeaseContractService leaseContractService,
            LeaseOrderService leaseOrderService,
            RepairRequestService repairRequestService,
            ComplaintService complaintService) {
        this.appointmentService = appointmentService;
        this.rentalApplicationService = rentalApplicationService;
        this.leaseContractService = leaseContractService;
        this.leaseOrderService = leaseOrderService;
        this.repairRequestService = repairRequestService;
        this.complaintService = complaintService;
    }

    @GetMapping("/appointments")
    public Result<List<Appointment>> appointments(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                appointmentService.tenantList(RequestUserUtil.getCurrentUserId(request))
        );
    }

    @GetMapping("/applications")
    public Result<List<RentalApplication>> applications(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                rentalApplicationService.tenantList(RequestUserUtil.getCurrentUserId(request))
        );
    }

    @GetMapping("/contracts")
    public Result<List<LeaseContract>> contracts(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                leaseContractService.tenantList(RequestUserUtil.getCurrentUserId(request))
        );
    }

    @GetMapping("/orders")
    public Result<List<LeaseOrder>> orders(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                leaseOrderService.tenantList(RequestUserUtil.getCurrentUserId(request))
        );
    }

    @GetMapping("/repairs")
    public Result<List<RepairRequest>> repairs(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                repairRequestService.tenantList(RequestUserUtil.getCurrentUserId(request))
        );
    }

    @GetMapping("/complaints")
    public Result<List<Complaint>> complaints(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(
                complaintService.userList(RequestUserUtil.getCurrentUserId(request))
        );
    }
}
