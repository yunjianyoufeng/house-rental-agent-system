package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.AgentConversationExchangeDTO;
import com.rental.dto.AgentConversationSummaryDTO;
import com.rental.entity.AgentConversation;
import com.rental.entity.AgentMessage;
import com.rental.exception.BusinessException;
import com.rental.mapper.AgentConversationMapper;
import com.rental.mapper.AgentMessageMapper;
import com.rental.service.AgentConversationService;
import com.rental.vo.AgentConversationContextVO;
import com.rental.vo.AgentConversationDetailVO;
import com.rental.vo.AgentConversationVO;
import com.rental.vo.AgentMessageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AgentConversationServiceImpl implements AgentConversationService {

    private static final int HISTORY_LIMIT = 30;
    private static final int DETAIL_MESSAGE_LIMIT = 100;
    private static final int CONTEXT_MESSAGE_LIMIT = 40;

    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;

    public AgentConversationServiceImpl(
            AgentConversationMapper conversationMapper,
            AgentMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public void saveExchange(Long userId, AgentConversationExchangeDTO dto) {
        String conversationId = dto.getConversationId().trim();
        AgentConversation conversation = findConversation(userId, conversationId);
        LocalDateTime now = LocalDateTime.now();
        if (conversation == null) {
            conversation = new AgentConversation();
            conversation.setConversationId(conversationId);
            conversation.setUserId(userId);
            conversation.setTitle(buildTitle(dto.getUserMessage()));
            conversation.setSummaryMessageId(0L);
            conversation.setStatus(1);
            conversation.setLastMessageTime(now);
            if (conversationMapper.insert(conversation) <= 0) {
                throw new BusinessException("Agent会话创建失败");
            }
        }

        insertMessage(userId, conversationId, "user", dto.getUserMessage(), null);
        insertMessage(
                userId,
                conversationId,
                "assistant",
                dto.getAssistantMessage(),
                dto.getSourcesJson()
        );
        conversation.setLastMessageTime(now);
        if (conversationMapper.updateById(conversation) <= 0) {
            throw new BusinessException("Agent会话更新时间失败");
        }
    }

    @Override
    public List<AgentConversationVO> list(Long userId) {
        List<AgentConversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<AgentConversation>()
                        .eq(AgentConversation::getUserId, userId)
                        .eq(AgentConversation::getStatus, 1)
                        .orderByDesc(AgentConversation::getLastMessageTime)
                        .last("LIMIT " + HISTORY_LIMIT)
        );
        return conversations.stream().map(this::toConversationVO).toList();
    }

    @Override
    public AgentConversationDetailVO detail(Long userId, String conversationId) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        List<AgentMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getUserId, userId)
                        .eq(AgentMessage::getConversationId, conversationId)
                        .orderByDesc(AgentMessage::getId)
                        .last("LIMIT " + DETAIL_MESSAGE_LIMIT)
        );
        Collections.reverse(messages);

        AgentConversationDetailVO detail = new AgentConversationDetailVO();
        detail.setConversationId(conversation.getConversationId());
        detail.setTitle(conversation.getTitle());
        detail.setMessages(messages.stream().map(this::toMessageVO).toList());
        return detail;
    }

    @Override
    public AgentConversationContextVO context(Long userId, String conversationId) {
        AgentConversation conversation = findConversation(userId, conversationId);
        AgentConversationContextVO context = new AgentConversationContextVO();
        if (conversation == null) {
            context.setSummary(null);
            context.setSummaryMessageId(0L);
            context.setMessages(List.of());
            return context;
        }

        long summaryMessageId = conversation.getSummaryMessageId() == null
                ? 0L : conversation.getSummaryMessageId();
        List<AgentMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getUserId, userId)
                        .eq(AgentMessage::getConversationId, conversationId)
                        .gt(AgentMessage::getId, summaryMessageId)
                        .orderByAsc(AgentMessage::getId)
                        .last("LIMIT " + CONTEXT_MESSAGE_LIMIT)
        );
        context.setSummary(conversation.getSummary());
        context.setSummaryMessageId(summaryMessageId);
        context.setMessages(messages.stream().map(this::toMessageVO).toList());
        return context;
    }

    @Override
    @Transactional
    public void updateSummary(
            Long userId,
            String conversationId,
            AgentConversationSummaryDTO dto) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        long currentMessageId = conversation.getSummaryMessageId() == null
                ? 0L : conversation.getSummaryMessageId();
        if (dto.getThroughMessageId() <= currentMessageId) {
            return;
        }

        AgentMessage throughMessage = messageMapper.selectById(dto.getThroughMessageId());
        if (throughMessage == null
                || !userId.equals(throughMessage.getUserId())
                || !conversationId.equals(throughMessage.getConversationId())) {
            throw new BusinessException("摘要覆盖的消息不属于当前会话");
        }
        conversation.setSummary(dto.getSummary().trim());
        conversation.setSummaryMessageId(dto.getThroughMessageId());
        if (conversationMapper.updateById(conversation) <= 0) {
            throw new BusinessException("Agent会话摘要更新失败");
        }
    }

    private AgentConversation findConversation(Long userId, String conversationId) {
        return conversationMapper.selectOne(
                new LambdaQueryWrapper<AgentConversation>()
                        .eq(AgentConversation::getUserId, userId)
                        .eq(AgentConversation::getConversationId, conversationId)
                        .last("LIMIT 1")
        );
    }

    private AgentConversation requireConversation(Long userId, String conversationId) {
        AgentConversation conversation = findConversation(userId, conversationId);
        if (conversation == null) {
            throw new BusinessException("Agent会话不存在或无权访问");
        }
        return conversation;
    }

    private void insertMessage(
            Long userId,
            String conversationId,
            String role,
            String content,
            String sourcesJson) {
        AgentMessage message = new AgentMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content.trim());
        message.setSourcesJson(sourcesJson);
        if (messageMapper.insert(message) <= 0) {
            throw new BusinessException("Agent消息保存失败");
        }
    }

    private String buildTitle(String message) {
        String title = message.trim().replaceAll("\\s+", " ");
        return title.length() <= 30 ? title : title.substring(0, 30) + "…";
    }

    private AgentConversationVO toConversationVO(AgentConversation conversation) {
        AgentConversationVO vo = new AgentConversationVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setTitle(conversation.getTitle());
        vo.setLastMessageTime(conversation.getLastMessageTime());
        vo.setCreateTime(conversation.getCreateTime());
        return vo;
    }

    private AgentMessageVO toMessageVO(AgentMessage message) {
        AgentMessageVO vo = new AgentMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSourcesJson(message.getSourcesJson());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
