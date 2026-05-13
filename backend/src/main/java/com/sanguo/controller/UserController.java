package com.sanguo.controller;

import com.sanguo.common.BizException;
import com.sanguo.common.Result;
import com.sanguo.entity.User;
import com.sanguo.security.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private com.sanguo.mapper.UserMapper userMapper;

    @ApiOperation("个人信息")
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userMapper.selectById(login.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("createTime", user.getCreateTime());
        return Result.ok(data);
    }

    @ApiOperation("更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userMapper.selectById(login.getUserId());
        if (body.containsKey("realName")) user.setRealName(body.get("realName"));
        if (body.containsKey("phone")) {
            String phone = body.get("phone");
            if (phone != null && !StringUtils.hasText(phone)) phone = null;
            if (phone != null) {
                Long cnt = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone).ne(User::getId, user.getId()));
                if (cnt != null && cnt > 0) throw new BizException("手机号已被使用");
            }
            user.setPhone(phone);
        }
        if (body.containsKey("email")) {
            String email = body.get("email");
            if (email != null && !StringUtils.hasText(email)) email = null;
            if (email != null) {
                Long cnt = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email).ne(User::getId, user.getId()));
                if (cnt != null && cnt > 0) throw new BizException("邮箱已被使用");
            }
            user.setEmail(email);
        }
        if (body.containsKey("avatarUrl")) user.setAvatarUrl(body.get("avatarUrl"));
        userMapper.updateById(user);
        return Result.ok();
    }
}
