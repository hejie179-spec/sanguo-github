package com.sanguo.controller;

import com.sanguo.common.BizException;
import com.sanguo.common.Constants;
import com.sanguo.common.Result;
import com.sanguo.dto.LoginDTO;
import com.sanguo.dto.RegisterDTO;
import com.sanguo.entity.User;
import com.sanguo.entity.UserRole;
import com.sanguo.mapper.RoleMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.mapper.UserRoleMapper;
import com.sanguo.security.JwtUtil;
import com.sanguo.security.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    // 认证相关的接口，处理登录、注册、获取用户信息
    // 接口列表：
    // 1. POST /auth/register：注册新用户，成功后直接返回token
    // 2. POST /auth/login：用户登录，返回token
    // 3. GET /auth/info：获取当前登录用户的信息，需要带token
    // 前端拿到token后会保存起来，之后每次请求都会在请求头里带上Authorization: Bearer <token>
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtUtil jwtUtil;

    @ApiOperation("注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Validated @RequestBody RegisterDTO dto) {
        // 处理选填字段，如果是空字符串就设置为null，避免重复校验出问题
        if (dto.getPhone() != null && !StringUtils.hasText(dto.getPhone())) dto.setPhone(null);
        if (dto.getEmail() != null && !StringUtils.hasText(dto.getEmail())) dto.setEmail(null);
        
        // 检查用户名是否已存在
        if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException("用户名已存在");
        }
        
        // 检查手机号是否已注册（如果填写了的话）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                    .eq(User::getPhone, dto.getPhone())) > 0) {
                throw new BizException("手机号已注册");
            }
        }
        
        // 检查邮箱是否已注册（如果填写了的话）
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                    .eq(User::getEmail, dto.getEmail())) > 0) {
                throw new BizException("邮箱已注册");
            }
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 密码要加密存储
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(Constants.USER_NORMAL); // 新用户默认是正常状态
        userMapper.insert(user);
        
        // 给新用户绑定默认角色：普通用户
        Integer roleUser = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.sanguo.entity.Role>()
                .eq(com.sanguo.entity.Role::getCode, Constants.ROLE_USER)).getId();
        UserRole ur = new UserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(roleUser);
        userRoleMapper.insert(ur);
        
        // 生成token并返回
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok(data);
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO dto) {
        User user = findUserForLogin(dto.getUsername(), dto.getPassword());
        
        // 检查账号是否被禁用
        if (user.getStatus() != null && user.getStatus() == Constants.USER_DISABLED) {
            throw new BizException(403, "账号已被禁用");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 生成token并返回用户信息
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("avatarUrl", user.getAvatarUrl()); // 顺便把头像地址也返回
        return Result.ok(data);
    }

    private User findUserForLogin(String account, String rawPassword) {
        String a = account == null ? "" : account.trim();
        if (!StringUtils.hasText(a)) {
            throw new BizException(401, "账号或密码错误");
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, a)
                .or().eq(User::getPhone, a)
                .or().eq(User::getEmail, a);
        java.util.List<User> candidates = userMapper.selectList(q);
        if (candidates == null || candidates.isEmpty()) {
            throw new BizException(401, "账号或密码错误");
        }

        java.util.List<User> matched = new java.util.ArrayList<>();
        for (User u : candidates) {
            if (u != null && passwordEncoder.matches(rawPassword, u.getPassword())) {
                matched.add(u);
            }
        }
        if (matched.isEmpty()) {
            throw new BizException(401, "账号或密码错误");
        }
        if (matched.size() > 1) {
            throw new BizException("该账号存在多条匹配记录，请使用用户名登录或联系管理员处理");
        }
        return matched.get(0);
    }

    @ApiOperation("当前用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        // JwtFilter会在请求进来时验证token，并把登录用户信息存到SecurityContext里
        // 所以这里直接从SecurityContext里取用户信息就行
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = login.getUser();
        
        // 把用户的各种信息打包返回
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("roles", login.getAuthorities().stream().map(a -> a.getAuthority()).toArray()); // 把用户角色也返回
        return Result.ok(data);
    }
}
