package com.rental.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentConversationVO {
    private String conversationId;
    private String title;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
}
