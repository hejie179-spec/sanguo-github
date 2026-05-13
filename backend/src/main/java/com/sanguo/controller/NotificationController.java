package com.sanguo.controller;

import com.sanguo.common.Result;
import com.sanguo.entity.Notification;
import com.sanguo.security.JwtUtil;
import com.sanguo.security.LoginUser;
import com.sanguo.security.UserDetailsServiceImpl;
import com.sanguo.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "通知")
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private NotificationService notificationService;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private UserDetailsServiceImpl userDetailsService;

    @ApiOperation("SSE实时订阅")
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam(required = false) String token) {
        if (token != null && !token.isEmpty()) {
            try {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {}
        }
        return notificationService.subscribe();
    }

    @ApiOperation("未读列表")
    @GetMapping("/unread")
    public Result<List<Notification>> unread() {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(notificationService.getUnread(login.getUserId()));
    }

    @ApiOperation("未读数量")
    @GetMapping("/unread/count")
    public Result<Integer> unreadCount() {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(notificationService.getUnreadCount(login.getUserId()));
    }

    @ApiOperation("标记已读")
    @PutMapping("/read/{id}")
    public Result<Void> read(@PathVariable Integer id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    @ApiOperation("全部已读")
    @PutMapping("/read/all")
    public Result<Void> readAll() {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        notificationService.markAllRead(login.getUserId());
        return Result.ok();
    }
}
