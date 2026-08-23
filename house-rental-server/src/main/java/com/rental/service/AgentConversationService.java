package com.rental.service;

import com.rental.dto.AgentConversationExchangeDTO;
import com.rental.dto.AgentConversationSummaryDTO;
import com.rental.vo.AgentConversationContextVO;
import com.rental.vo.AgentConversationDetailVO;
import com.rental.vo.AgentConversationVO;

import java.util.List;

public interface AgentConversationService {

    void saveExchange(Long userId, AgentConversationExchangeDTO dto);

    List<AgentConversationVO> list(Long userId);

    AgentConversationDetailVO detail(Long userId, String conversationId);

    AgentConversationContextVO context(Long userId, String conversationId);

    void updateSummary(Long userId, String conversationId, AgentConversationSummaryDTO dto);

    void delete(Long userId, String conversationId);
}
