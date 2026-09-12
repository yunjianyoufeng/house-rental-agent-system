package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RentDemandParseDTO {

    @NotBlank(message = "请输入租房需求")
    @Size(max = 2000, message = "租房需求不能超过2000字")
    private String query;

    /**
     * 是否使用大语言模型解析。
     * true：优先 DeepSeek
     * false：只使用本地规则解析
     */
    private Boolean useLlm = true;
}
