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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

@Service
public class AgentConversationServiceImpl implements AgentConversationService {

    private static final Logger log = LoggerFactory.getLogger(AgentConversationServiceImpl.class);
    private static final int HISTORY_LIMIT = 30;
    private static final int DETAIL_MESSAGE_LIMIT = 100;
    private static final int CONTEXT_MESSAGE_LIMIT = 40;

    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AgentConversationServiceImpl(
            AgentConversationMapper conversationMapper,
            AgentMessageMapper messageMapper,
            StringRedisTemplate stringRedisTemplate) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.stringRedisTemplate = stringRedisTemplate;
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

    @Override
    @Transactional
    public void delete(Long userId, String conversationId) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        messageMapper.delete(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getUserId, userId)
                        .eq(AgentMessage::getConversationId, conversationId)
        );
        if (conversationMapper.deleteById(conversation.getId()) <= 0) {
            throw new BusinessException("Agent历史对话删除失败");
        }
        clearRedisConversationState(userId, conversationId);
    }

    private void clearRedisConversationState(Long userId, String conversationId) {
        String conversationHash = sha256(conversationId);
        List<String> keys = List.of(
                "agent:conversation:" + userId + ":" + conversationHash,
                "agent:appointment:" + userId + ":" + conversationHash
        );
        try {
            stringRedisTemplate.delete(keys);
        } catch (RuntimeException exception) {
            log.warn("Agent历史已从MySQL删除，但Redis会话缓存清理失败：userId={}, conversationId={}",
                    userId, conversationId, exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前Java环境不支持SHA-256", exception);
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
