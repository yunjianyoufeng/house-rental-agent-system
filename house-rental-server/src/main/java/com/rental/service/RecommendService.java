package com.rental.service;

import com.rental.dto.HouseRecommendDTO;
import com.rental.vo.HouseRecommendVO;

import java.util.List;

public interface RecommendService {

    List<HouseRecommendVO> recommendHouse(HouseRecommendDTO dto);
}