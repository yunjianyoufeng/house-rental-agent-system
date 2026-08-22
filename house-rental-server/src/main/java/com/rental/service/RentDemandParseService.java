package com.rental.service;

import com.rental.dto.RentDemandParseDTO;
import com.rental.vo.RentDemandParseVO;

public interface RentDemandParseService {

    RentDemandParseVO parseDemand(RentDemandParseDTO dto);
}