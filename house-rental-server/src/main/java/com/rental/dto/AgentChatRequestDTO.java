package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    private String message;

    @Size(max = 100, message = "会话标识不能超过100个字符")
    private String conversationId;

    /**
     * 由后端根据当前登录状态写入，不信任前端传值。
     */
    private Long userId;

    /**
     * 由后端根据当前登录状态写入，不信任前端传值。
     */
    private String roleCode;
}
