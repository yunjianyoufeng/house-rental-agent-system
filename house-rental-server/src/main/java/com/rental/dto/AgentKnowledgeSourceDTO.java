package com.rental.dto;

import lombok.Data;

@Data
public class AgentKnowledgeSourceDTO {

    private String source;

    private String title;

    private String section;

    private Double relevance;
}
