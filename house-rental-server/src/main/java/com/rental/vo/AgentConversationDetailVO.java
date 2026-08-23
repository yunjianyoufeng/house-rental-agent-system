package com.rental.vo;

import lombok.Data;

import java.util.List;

@Data
public class AgentConversationDetailVO {
    private String conversationId;
    private String title;
    private List<AgentMessageVO> messages;
}
