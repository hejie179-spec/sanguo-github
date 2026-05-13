package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Role;
import com.sanguo.entity.User;
import com.sanguo.entity.UserRole;
import com.sanguo.mapper.RoleMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.mapper.UserRoleMapper;
import com.sanguo.security.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "管理-用户")
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @ApiOperation("角色列表")
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> listRoles() {
        List<Role> roles = roleMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role r : roles) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("code", r.getCode());
            m.put("name", r.getName());
            result.add(m);
        }
        return Result.ok(result);
    }

    @ApiOperation("用户分页")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(User::getUsername, keyword).or().like(User::getPhone, keyword).or().like(User::getEmail, keyword));
        }
        if (status != null) q.eq(User::getStatus, status);
        q.orderByAsc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), q);
        List<User> users = page.getRecords();
        List<Integer> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Integer, String> roleIdToCode = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode, (a, b) -> a));
        Map<Integer, List<String>> userAuthorities = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
            for (UserRole ur : urs) {
                String code = roleIdToCode.get(ur.getRoleId());
                if (code == null) continue;
                userAuthorities.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(code.toUpperCase());
            }
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("avatarUrl", u.getAvatarUrl());
            m.put("status", u.getStatus());
            m.put("lastLoginTime", u.getLastLoginTime());
            m.put("createTime", u.getCreateTime());
            m.put("updateTime", u.getUpdateTime());
            List<String> roles = userAuthorities.getOrDefault(u.getId(), new ArrayList<>());
            if (roles.isEmpty()) roles.add("USER");
            m.put("roles", roles);
            records.add(m);
        }
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    @ApiOperation("禁用/启用")
    @PutMapping("/status/{id}")
    public Result<Void> setStatus(@PathVariable Integer id, @RequestParam Integer status) {
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");
        u.setStatus(status);
        userMapper.updateById(u);
        return Result.ok();
    }

    @ApiOperation("重置密码")
    @PutMapping("/resetPwd/{id}")
    public Result<Void> resetPwd(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        String newPwd = body.get("password");
        if (newPwd == null || newPwd.length() < 6) {
            return Result.fail("密码至少6位");
        }
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");
        u.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(u);
        return Result.ok();
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (login.getUserId().equals(id)) return Result.fail("不能删除自己");
        User target = userMapper.selectById(id);
        if (target == null) return Result.fail("用户不存在");
        if (hasRole(id, Constants.ROLE_SUPER_ADMIN) && countByRole(Constants.ROLE_SUPER_ADMIN) <= 1) {
            return Result.fail("至少保留一个超级管理员");
        }
        try {
            userMapper.deleteById(id);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除失败：该用户可能存在关联数据");
        }
    }

    @ApiOperation("设置用户角色")
    @PutMapping("/role/{id}")
    public Result<Void> setRoles(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (login.getUserId().equals(id)) return Result.fail("不能修改自己的角色");
        User target = userMapper.selectById(id);
        if (target == null) return Result.fail("用户不存在");
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds == null) roleIds = new ArrayList<>();
        Integer superAdminRoleId = getRoleId(Constants.ROLE_SUPER_ADMIN);
        Integer userRoleId = getRoleId(Constants.ROLE_USER);

        // 至少保留一个超级管理员：当目标用户原本是超级管理员，但本次要移除超级管理员权限时，需要拦截
        if (superAdminRoleId != null) {
            boolean targetIsSuperAdmin = hasRole(id, Constants.ROLE_SUPER_ADMIN);
            boolean willBeSuperAdmin = roleIds.contains(superAdminRoleId);
            if (targetIsSuperAdmin && !willBeSuperAdmin && countByRole(Constants.ROLE_SUPER_ADMIN) <= 1) {
                return Result.fail("至少保留一个超级管理员");
            }
        }

        // 为了保证账号最少有普通用户角色，强制补齐 user 角色
        if (userRoleId != null && !roleIds.contains(userRoleId)) {
            roleIds.add(userRoleId);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        for (Integer rid : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(id);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
        return Result.ok();
    }

    @ApiOperation("创建管理员账号")
    @PostMapping("/createAdmin")
    public Result<Integer> createAdmin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String phone = body.get("phone");
        String email = body.get("email");
        String roleCode = body.getOrDefault("roleCode", Constants.ROLE_ADMIN);
        if (!StringUtils.hasText(username) || username.length() < 2) return Result.fail("用户名至少2位");
        if (!StringUtils.hasText(password) || password.length() < 6) return Result.fail("密码至少6位");
        if (phone != null && !StringUtils.hasText(phone)) phone = null;
        if (email != null && !StringUtils.hasText(email)) email = null;
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) return Result.fail("用户名已存在");
        if (phone != null && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) return Result.fail("手机号已注册");
        if (email != null && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) return Result.fail("邮箱已注册");

        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setPhone(phone);
        u.setEmail(email);
        u.setStatus(Constants.USER_NORMAL);
        userMapper.insert(u);

        Integer roleUserId = getRoleId(Constants.ROLE_USER);
        Integer roleId = getRoleId(roleCode);
        if (roleUserId != null) {
            UserRole ur = new UserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(roleUserId);
            userRoleMapper.insert(ur);
        }
        if (roleId != null) {
            UserRole ur = new UserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return Result.ok(u.getId());
    }

    private Integer getRoleId(String code) {
        Role r = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        return r == null ? null : r.getId();
    }

    private boolean hasRole(Integer userId, String roleCode) {
        Integer roleId = getRoleId(roleCode);
        if (roleId == null) return false;
        return userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, roleId)) > 0;
    }

    private long countByRole(String roleCode) {
        Integer roleId = getRoleId(roleCode);
        if (roleId == null) return 0;
        return userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
    }
}
