package com.sanguo.security;

import com.sanguo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoginUser implements UserDetails {

    private final User user;
    private final List<String> roles;

    public LoginUser(User user, List<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    public User getUser() {
        return user;
    }

    public Integer getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> {
                    String roleName = r == null ? "" : r.trim();
                    if (roleName.startsWith("ROLE_")) {
                        roleName = roleName.substring("ROLE_".length());
                    }
                    roleName = roleName.toUpperCase();
                    return new SimpleGrantedAuthority("ROLE_" + roleName);
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == 1;
    }
}
