package com.sanguo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50)
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20)
    private String password;
    private String realName;
    private String phone;
    private String email;
}
