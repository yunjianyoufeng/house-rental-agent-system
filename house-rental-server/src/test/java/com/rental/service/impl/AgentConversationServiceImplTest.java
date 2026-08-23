package com.rental.service.impl;

import com.rental.dto.AgentConversationExchangeDTO;
import com.rental.entity.AgentConversation;
import com.rental.entity.AgentMessage;
import com.rental.mapper.AgentConversationMapper;
import com.rental.mapper.AgentMessageMapper;
import com.rental.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConversationServiceImplTest {

    private AgentConversationMapper conversationMapper;
    private AgentMessageMapper messageMapper;
    private StringRedisTemplate stringRedisTemplate;
    private AgentConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(AgentConversationMapper.class);
        messageMapper = mock(AgentMessageMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        service = new AgentConversationServiceImpl(
                conversationMapper,
                messageMapper,
                stringRedisTemplate
        );
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

    @Test
    void deleteRemovesOwnedMessagesConversationAndRedisState() {
        AgentConversation conversation = new AgentConversation();
        conversation.setId(21L);
        conversation.setUserId(8L);
        conversation.setConversationId("conversation-1");
        when(conversationMapper.selectOne(any())).thenReturn(conversation);
        when(messageMapper.delete(any())).thenReturn(2);
        when(conversationMapper.deleteById(21L)).thenReturn(1);

        service.delete(8L, "conversation-1");

        verify(messageMapper).delete(any());
        verify(conversationMapper).deleteById(21L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(stringRedisTemplate).delete(keysCaptor.capture());
        assertEquals(2, keysCaptor.getValue().size());
    }

    @Test
    void deleteRejectsMissingOrForeignConversation() {
        when(conversationMapper.selectOne(any())).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> service.delete(8L, "foreign-conversation")
        );

        verify(messageMapper, never()).delete(any());
        verify(conversationMapper, never()).deleteById(anyLong());
        verify(stringRedisTemplate, never()).delete(any(Collection.class));
    }
}
