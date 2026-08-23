package com.rental.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentMessageVO {
    private Long id;
    private String role;
    private String content;
    private String sourcesJson;
    private LocalDateTime createTime;
}
