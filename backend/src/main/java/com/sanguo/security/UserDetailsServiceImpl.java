package com.sanguo.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.common.Constants;
import com.sanguo.entity.Role;
import com.sanguo.entity.User;
import com.sanguo.entity.UserRole;
import com.sanguo.mapper.RoleMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.mapper.UserRoleMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username 可能是 userId（JWT 里存的）或 用户名/手机/邮箱
        User user = null;
        try {
            int id = Integer.parseInt(username);
            user = userMapper.selectById(id);
        } catch (NumberFormatException e) {
            user = findUserByAccount(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<UserRole> urList = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<String> roleCodes = urList.stream()
                .map(ur -> roleMapper.selectById(ur.getRoleId()))
                .filter(r -> r != null)
                .map(Role::getCode)
                .collect(Collectors.toList());
        if (roleCodes.isEmpty()) {
            roleCodes.add(Constants.ROLE_USER);
        }
        return new LoginUser(user, roleCodes);
    }

    private User findUserByAccount(String account) {
        String a = account == null ? "" : account.trim();
        if (!StringUtils.hasText(a)) return null;
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (a.contains("@")) {
            q.eq(User::getEmail, a);
        } else if (a.matches("^\\d{6,20}$")) {
            q.eq(User::getPhone, a);
        } else {
            q.eq(User::getUsername, a);
        }
        List<User> list = userMapper.selectList(q);
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }
}
