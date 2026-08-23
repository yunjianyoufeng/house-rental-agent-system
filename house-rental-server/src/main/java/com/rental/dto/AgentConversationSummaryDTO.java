package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentConversationSummaryDTO {

    @NotBlank
    @Size(max = 4000)
    private String summary;

    @NotNull
    @Positive
    private Long throughMessageId;
}
