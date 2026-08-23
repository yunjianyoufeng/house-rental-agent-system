package com.rental.controller;

import com.rental.client.AgentClient;
import com.rental.dto.AgentChatRequestDTO;
import com.rental.dto.AgentChatResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentControllerSecurityTest {

    @Test
    void chatOverwritesUntrustedIdentityWithLoginContext() {
        AgentClient agentClient = mock(AgentClient.class);
        AgentController controller = new AgentController(agentClient);
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setAnswer("ok");
        when(agentClient.chat(any(), eq("Bearer token"))).thenReturn(response);

        AgentChatRequestDTO dto = new AgentChatRequestDTO();
        dto.setMessage("我的合同有哪些");
        dto.setUserId(999L);
        dto.setRoleCode("ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        request.setAttribute("currentUserId", 8L);
        request.setAttribute("currentUserRole", "TENANT");

        controller.chat(dto, request);

        ArgumentCaptor<AgentChatRequestDTO> captor =
                ArgumentCaptor.forClass(AgentChatRequestDTO.class);
        verify(agentClient).chat(captor.capture(), eq("Bearer token"));
        assertEquals(8L, captor.getValue().getUserId());
        assertEquals("TENANT", captor.getValue().getRoleCode());
    }
}
