package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 12, max = 72, message = "密码长度须为12至72个字符")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50)
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 100)
    private String email;
}
