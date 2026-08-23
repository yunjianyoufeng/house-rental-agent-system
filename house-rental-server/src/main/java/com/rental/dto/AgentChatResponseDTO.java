package com.rental.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentChatResponseDTO {

    private String answer;

    private String conversationId;

    private List<AgentKnowledgeSourceDTO> sources = new ArrayList<>();
}
