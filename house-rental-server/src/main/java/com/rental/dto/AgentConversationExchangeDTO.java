package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentConversationExchangeDTO {

    @NotBlank
    @Size(max = 100)
    private String conversationId;

    @NotBlank
    @Size(max = 2000)
    private String userMessage;

    @NotBlank
    @Size(max = 20000)
    private String assistantMessage;

    @Size(max = 20000)
    private String sourcesJson;
}
