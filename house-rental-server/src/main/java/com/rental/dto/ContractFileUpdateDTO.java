package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractFileUpdateDTO {

    @NotBlank(message = "合同文件地址不能为空")
    private String contractUrl;
}
