package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.dto.AgentConversationExchangeDTO;
import com.rental.dto.AgentConversationSummaryDTO;
import com.rental.service.AgentConversationService;
import com.rental.vo.AgentConversationContextVO;
import com.rental.vo.AgentConversationDetailVO;
import com.rental.vo.AgentConversationVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenant/agent-history")
public class AgentConversationController {

    private final AgentConversationService conversationService;

    public AgentConversationController(AgentConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public Result<List<AgentConversationVO>> list(HttpServletRequest request) {
        return Result.success(conversationService.list(currentTenantId(request)));
    }

    @GetMapping("/{conversationId}")
    public Result<AgentConversationDetailVO> detail(
            @PathVariable String conversationId,
            HttpServletRequest request) {
        return Result.success(
                conversationService.detail(currentTenantId(request), conversationId)
        );
    }

    @GetMapping("/{conversationId}/context")
    public Result<AgentConversationContextVO> context(
            @PathVariable String conversationId,
            HttpServletRequest request) {
        return Result.success(
                conversationService.context(currentTenantId(request), conversationId)
        );
    }

    @PostMapping("/exchanges")
    public Result<String> saveExchange(
            @RequestBody @Valid AgentConversationExchangeDTO dto,
            HttpServletRequest request) {
        conversationService.saveExchange(currentTenantId(request), dto);
        return Result.success("Agent对话已保存");
    }

    @PutMapping("/{conversationId}/summary")
    public Result<String> updateSummary(
            @PathVariable String conversationId,
            @RequestBody @Valid AgentConversationSummaryDTO dto,
            HttpServletRequest request) {
        conversationService.updateSummary(currentTenantId(request), conversationId, dto);
        return Result.success("Agent对话摘要已更新");
    }

    private Long currentTenantId(HttpServletRequest request) {
        RequestUserUtil.checkTenantRole(request);
        return RequestUserUtil.getCurrentUserId(request);
    }
}
