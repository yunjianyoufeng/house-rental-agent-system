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
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chatStream(
            @RequestBody @Valid AgentChatRequestDTO dto,
            HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        dto.setUserId(RequestUserUtil.getCurrentUserId(request));
        String authorization = request.getHeader("Authorization");
        StreamingResponseBody responseBody = outputStream ->
                agentClient.stream(dto, authorization, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .body(responseBody);
    }
}
