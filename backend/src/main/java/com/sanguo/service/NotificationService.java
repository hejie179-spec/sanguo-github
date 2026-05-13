package com.sanguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.entity.Notification;
import com.sanguo.entity.Role;
import com.sanguo.entity.UserRole;
import com.sanguo.mapper.NotificationMapper;
import com.sanguo.mapper.RoleMapper;
import com.sanguo.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void notifyReviewAdmins(String type, String content, String link) {
        List<Integer> adminIds = getAdminUserIds();
        for (Integer adminId : adminIds) {
            Notification n = new Notification();
            n.setUserId(adminId);
            n.setType(type);
            n.setContent(content);
            n.setLink(link);
            n.setStatus(0);
            n.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(n);
        }
        broadcast(type, content, link);
    }

    private List<Integer> getAdminUserIds() {
        List<Role> adminRoles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getCode, "super_admin", "review_admin", "admin"));
        if (adminRoles.isEmpty()) return new ArrayList<>();
        List<Integer> roleIds = adminRoles.stream().map(Role::getId).collect(Collectors.toList());
        List<UserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getRoleId, roleIds));
        return urs.stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
    }

    public List<Notification> getUnread(Integer userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, 0)
                .orderByDesc(Notification::getCreateTime));
    }

    public int getUnreadCount(Integer userId) {
        return Math.toIntExact(notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, 0)));
    }

    public void markRead(Integer id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null) {
            n.setStatus(1);
            notificationMapper.updateById(n);
        }
    }

    public void markAllRead(Integer userId) {
        List<Notification> list = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getStatus, 0));
        for (Notification n : list) {
            n.setStatus(1);
            notificationMapper.updateById(n);
        }
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    private void broadcast(String type, String content, String link) {
        String data = type + "|" + content + "|" + (link != null ? link : "");
        for (SseEmitter em : emitters) {
            try {
                em.send(SseEmitter.event().data(data));
            } catch (Exception ignored) {
                emitters.remove(em);
            }
        }
    }
}
