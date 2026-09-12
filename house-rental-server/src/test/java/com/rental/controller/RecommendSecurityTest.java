package com.rental.controller;

import com.rental.dto.HouseRecommendDTO;
import com.rental.service.RecommendService;
import com.rental.service.RequestLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendSecurityTest {
    private final RecommendService recommendations = mock(RecommendService.class);
    private final RequestLimitService limits = mock(RequestLimitService.class);
    private final RecommendController controller = new RecommendController(recommendations, limits);

    @Test
    void anonymousCannotEnablePaidParsingOrEmbedding() {
        HouseRecommendDTO dto = new HouseRecommendDTO();
        dto.setUseLlmParse(true);
        dto.setModelType("EMBEDDING");
        controller.recommendHouse(dto, new MockHttpServletRequest());
        assertFalse(dto.getUseLlmParse());
        assertEquals("RULE", dto.getModelType());
        verify(recommendations).recommendHouse(dto);
    }

    @Test
    void authenticatedTenantMustPassQuotaBeforePaidParsing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("currentUserRole", "TENANT");
        request.setAttribute("currentUserId", 8L);
        HouseRecommendDTO dto = new HouseRecommendDTO();
        controller.recommendHouse(dto, request);
        assertTrue(dto.getUseLlmParse());
        verify(limits).check("llm-user", "8", 20, Duration.ofHours(1));
    }
}
