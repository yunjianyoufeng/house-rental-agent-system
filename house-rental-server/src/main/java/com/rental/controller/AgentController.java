package com.rental.controller;

import com.rental.client.AgentClient;
import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.AgentChatRequestDTO;
import com.rental.dto.AgentChatResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentClient agentClient;

    public AgentController(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    @PostMapping("/chat")
    public Result<AgentChatResponseDTO> chat(
            @RequestBody @Valid AgentChatRequestDTO dto,
            HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setUserId(RequestUserUtil.getCurrentUserId(request));
        return Result.success(agentClient.chat(dto, request.getHeader("Authorization")));
    }
}
