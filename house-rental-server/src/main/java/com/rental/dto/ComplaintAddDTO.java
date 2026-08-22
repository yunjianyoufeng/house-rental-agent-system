package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintAddDTO {

    private Long userId;

    private Long targetId;

    @NotBlank(message = "投诉内容不能为空")
    private String content;
}
