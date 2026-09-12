package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.RentDemandParseDTO;
import com.rental.service.RentDemandParseService;
import com.rental.service.RequestLimitService;
import java.time.Duration;
import com.rental.vo.RentDemandParseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend")
public class RentDemandParseController {

    private final RentDemandParseService rentDemandParseService;
    private final RequestLimitService requestLimitService;

    public RentDemandParseController(RentDemandParseService rentDemandParseService, RequestLimitService requestLimitService) {
        this.rentDemandParseService = rentDemandParseService;
        this.requestLimitService = requestLimitService;
    }

    @PostMapping("/parse-demand")
    public Result<RentDemandParseVO> parseDemand(@RequestBody @Valid RentDemandParseDTO dto,
                                                  HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        if (!Boolean.FALSE.equals(dto.getUseLlm())) {
            requestLimitService.check("llm-user", String.valueOf(RequestUserUtil.getCurrentUserId(request)),
                    20, Duration.ofHours(1));
        }
        return Result.success(rentDemandParseService.parseDemand(dto));
    }
}
