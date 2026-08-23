package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_conversation")
public class AgentConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private Long userId;
    private String title;
    private String summary;
    private Long summaryMessageId;
    private Integer status;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
