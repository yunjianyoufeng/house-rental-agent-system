package com.rental.vo;

import lombok.Data;

import java.util.List;

@Data
public class AgentConversationContextVO {
    private String summary;
    private Long summaryMessageId;
    private List<AgentMessageVO> messages;
}
