package com.rental.service.impl;

import com.rental.dto.AgentConversationExchangeDTO;
import com.rental.entity.AgentConversation;
import com.rental.entity.AgentMessage;
import com.rental.mapper.AgentConversationMapper;
import com.rental.mapper.AgentMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConversationServiceImplTest {

    private AgentConversationMapper conversationMapper;
    private AgentMessageMapper messageMapper;
    private AgentConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(AgentConversationMapper.class);
        messageMapper = mock(AgentMessageMapper.class);
        service = new AgentConversationServiceImpl(conversationMapper, messageMapper);
    }

    @Test
    void saveExchangeCreatesConversationAndTwoRoleMessages() {
        when(conversationMapper.selectOne(any())).thenReturn(null);
        when(conversationMapper.insert(any(AgentConversation.class))).thenReturn(1);
        when(conversationMapper.updateById(any(AgentConversation.class))).thenReturn(1);
        when(messageMapper.insert(any(AgentMessage.class))).thenReturn(1);

        AgentConversationExchangeDTO dto = new AgentConversationExchangeDTO();
        dto.setConversationId("conversation-1");
        dto.setUserMessage("帮我找历下区的房子");
        dto.setAssistantMessage("为您找到两套房源");
        dto.setSourcesJson("[]");

        service.saveExchange(8L, dto);

        ArgumentCaptor<AgentMessage> messageCaptor = ArgumentCaptor.forClass(AgentMessage.class);
        verify(messageMapper, times(2)).insert(messageCaptor.capture());
        List<AgentMessage> messages = messageCaptor.getAllValues();
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());
        assertEquals(8L, messages.get(0).getUserId());
        verify(conversationMapper).insert(any(AgentConversation.class));
    }

    @Test
    void contextForNewConversationReturnsEmptyState() {
        when(conversationMapper.selectOne(any())).thenReturn(null);

        var context = service.context(8L, "new-conversation");

        assertEquals(0L, context.getSummaryMessageId());
        assertEquals(List.of(), context.getMessages());
    }
}
