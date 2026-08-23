package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.RentDemandParseDTO;
import com.rental.service.RentDemandParseService;
import com.rental.vo.RentDemandParseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend")
public class RentDemandParseController {

    private final RentDemandParseService rentDemandParseService;

    public RentDemandParseController(RentDemandParseService rentDemandParseService) {
        this.rentDemandParseService = rentDemandParseService;
    }

    @PostMapping("/parse-demand")
    public Result<RentDemandParseVO> parseDemand(@RequestBody @Valid RentDemandParseDTO dto,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return Result.success(rentDemandParseService.parseDemand(dto));
    }
}
