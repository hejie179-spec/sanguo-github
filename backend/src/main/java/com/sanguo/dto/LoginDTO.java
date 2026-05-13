package com.sanguo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginDTO {
    @NotBlank(message = "账号不能为空")
    private String username;  // 用户名/手机/邮箱
    @NotBlank(message = "密码不能为空")
    private String password;
}
