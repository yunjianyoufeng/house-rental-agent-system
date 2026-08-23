package com.rental.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentChatResponseDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeKnowledgeSourcesFromAgentResponse() throws Exception {
        String json = """
                {
                  "answer": "可以在合同生效期间提交报修。",
                  "conversationId": "conversation-1",
                  "sources": [
                    {
                      "source": "repair-and-complaint.md",
                      "title": "报修与投诉规则",
                      "section": "报修提交条件",
                      "relevance": 0.82
                    }
                  ]
                }
                """;

        AgentChatResponseDTO response = objectMapper.readValue(
                json,
                AgentChatResponseDTO.class
        );

        assertEquals("conversation-1", response.getConversationId());
        assertNotNull(response.getSources());
        assertEquals(1, response.getSources().size());
        assertEquals("repair-and-complaint.md", response.getSources().get(0).getSource());
        assertEquals("报修提交条件", response.getSources().get(0).getSection());
    }
}
