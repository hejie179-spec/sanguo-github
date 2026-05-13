package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.ForumReply;
import com.sanguo.entity.ForumTopic;
import com.sanguo.entity.ForumTopicLike;
import com.sanguo.entity.User;
import com.sanguo.mapper.ForumReplyMapper;
import com.sanguo.mapper.ForumTopicMapper;
import com.sanguo.mapper.ForumTopicLikeMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.security.LoginUser;
import com.sanguo.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Api(tags = "论坛")
@RestController
@RequestMapping("/forum")
public class ForumController {

    @Resource
    private ForumTopicMapper topicMapper;
    @Resource
    private ForumReplyMapper replyMapper;
    @Resource
    private ForumTopicLikeMapper topicLikeMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NotificationService notificationService;

    @ApiOperation("主题分页列表")
    @GetMapping("/topic/list")
    public Result<PageResult<Map<String, Object>>> topicList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean topOnly) {
        LambdaQueryWrapper<ForumTopic> q = new LambdaQueryWrapper<>();
        q.eq(ForumTopic::getStatus, Constants.TOPIC_NORMAL);
        if (StringUtils.hasText(keyword)) q.like(ForumTopic::getTitle, keyword);
        if (StringUtils.hasText(category)) q.eq(ForumTopic::getCategory, category);
        if (Boolean.TRUE.equals(topOnly)) q.eq(ForumTopic::getTopStatus, 1);
        q.orderByDesc(ForumTopic::getTopStatus).orderByDesc(ForumTopic::getEssenceStatus).orderByDesc(ForumTopic::getCreateTime);
        Page<ForumTopic> page = topicMapper.selectPage(new Page<>(current, size), q);
        List<Map<String, Object>> records = new ArrayList<>();
        for (ForumTopic t : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("userId", t.getUserId());
            m.put("category", t.getCategory());
            m.put("viewCount", t.getViewCount());
            m.put("replyCount", t.getReplyCount());
            m.put("likeCount", t.getLikeCount());
            m.put("topStatus", t.getTopStatus());
            m.put("essenceStatus", t.getEssenceStatus());
            m.put("createTime", t.getCreateTime());
            User u = userMapper.selectById(t.getUserId());
            m.put("authorName", u != null ? u.getUsername() : "");
            records.add(m);
        }
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    @ApiOperation("主题详情")
    @GetMapping("/topic/detail/{id}")
    public Result<Map<String, Object>> topicDetail(@PathVariable Integer id) {
        ForumTopic t = topicMapper.selectById(id);
        if (t == null || t.getStatus() == null || t.getStatus() != Constants.TOPIC_NORMAL) {
            return Result.fail("主题不存在或已关闭");
        }
        t.setViewCount((t.getViewCount() == null ? 0 : t.getViewCount()) + 1);
        topicMapper.updateById(t);
        Map<String, Object> data = new HashMap<>();
        data.put("topic", t);
        User u = userMapper.selectById(t.getUserId());
        data.put("authorName", u != null ? u.getUsername() : "");
        boolean liked = false;
        try {
            LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            liked = topicLikeMapper.selectCount(new LambdaQueryWrapper<ForumTopicLike>()
                    .eq(ForumTopicLike::getTopicId, id).eq(ForumTopicLike::getUserId, login.getUserId())) > 0;
        } catch (Exception ignored) {}
        data.put("liked", liked);
        return Result.ok(data);
    }

    @ApiOperation("回复列表-树形")
    @GetMapping("/reply/list/{topicId}")
    public Result<List<Map<String, Object>>> replyList(@PathVariable Integer topicId) {
        LambdaQueryWrapper<ForumReply> q = new LambdaQueryWrapper<>();
        q.eq(ForumReply::getTopicId, topicId).eq(ForumReply::getStatus, 1).eq(ForumReply::getParentId, 0)
                .orderByAsc(ForumReply::getCreateTime);
        List<ForumReply> roots = replyMapper.selectList(q);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ForumReply r : roots) {
            list.add(buildReplyNode(r));
        }
        return Result.ok(list);
    }

    private Map<String, Object> buildReplyNode(ForumReply r) {
        Map<String, Object> m = new HashMap<>();
        User u = userMapper.selectById(r.getUserId());
        m.put("id", r.getId());
        m.put("userId", r.getUserId());
        m.put("username", u != null ? u.getUsername() : "");
        m.put("content", r.getContent());
        m.put("likeCount", r.getLikeCount());
        m.put("createTime", r.getCreateTime());
        List<ForumReply> children = replyMapper.selectList(new LambdaQueryWrapper<ForumReply>()
                .eq(ForumReply::getTopicId, r.getTopicId()).eq(ForumReply::getParentId, r.getId()).eq(ForumReply::getStatus, 1)
                .orderByAsc(ForumReply::getCreateTime));
        List<Map<String, Object>> childList = new ArrayList<>();
        for (ForumReply c : children) childList.add(buildReplyNode(c));
        m.put("children", childList);
        return m;
    }

    @ApiOperation("发布主题")
    @PostMapping("/topic/save")
    public Result<Integer> saveTopic(@RequestBody ForumTopic topic) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        topic.setUserId(login.getUserId());
        topic.setStatus(Constants.TOPIC_NORMAL);
        topic.setViewCount(0);
        topic.setReplyCount(0);
        topic.setLikeCount(0);
        topic.setTopStatus(0);
        topic.setEssenceStatus(0);
        topicMapper.insert(topic);
        notificationService.notifyReviewAdmins("forum_topic_submitted",
                "用户「" + login.getUsername() + "」发布了新论坛主题：「" + topic.getTitle() + "」",
                "/admin/forum");
        return Result.ok(topic.getId());
    }

    @ApiOperation("主题点赞/取消赞")
    @PostMapping("/topic/like/{id}")
    public Result<Boolean> likeTopic(@PathVariable Integer id) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ForumTopic t = topicMapper.selectById(id);
        if (t == null || t.getStatus() == null || t.getStatus() != Constants.TOPIC_NORMAL) {
            return Result.fail("主题不存在或已关闭");
        }
        ForumTopicLike existing = topicLikeMapper.selectOne(new LambdaQueryWrapper<ForumTopicLike>()
                .eq(ForumTopicLike::getTopicId, id).eq(ForumTopicLike::getUserId, login.getUserId()));
        boolean liked;
        if (existing != null) {
            topicLikeMapper.deleteById(existing.getId());
            t.setLikeCount(Math.max(0, (t.getLikeCount() == null ? 0 : t.getLikeCount()) - 1));
            liked = false;
        } else {
            ForumTopicLike tl = new ForumTopicLike();
            tl.setTopicId(id);
            tl.setUserId(login.getUserId());
            topicLikeMapper.insert(tl);
            t.setLikeCount((t.getLikeCount() == null ? 0 : t.getLikeCount()) + 1);
            liked = true;
        }
        topicMapper.updateById(t);
        return Result.ok(liked);
    }

    @ApiOperation("发布回复")
    @PostMapping("/reply/save")
    public Result<ForumReply> saveReply(@RequestBody ForumReply reply) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reply.setUserId(login.getUserId());
        reply.setParentId(reply.getParentId() != null ? reply.getParentId() : 0);
        reply.setStatus(1);
        reply.setLikeCount(0);
        replyMapper.insert(reply);
        ForumTopic t = topicMapper.selectById(reply.getTopicId());
        if (t != null) {
            t.setReplyCount((t.getReplyCount() == null ? 0 : t.getReplyCount()) + 1);
            topicMapper.updateById(t);
        }
        return Result.ok(reply);
    }

    @ApiOperation("删除回复-本人或管理员(隐藏)")
    @DeleteMapping("/reply/{id}")
    public Result<Void> deleteReply(@PathVariable Integer id) {
        ForumReply r = replyMapper.selectById(id);
        if (r == null) return Result.fail("回复不存在");
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = login.getAuthorities().stream().anyMatch(a ->
                "ROLE_SUPER_ADMIN".equals(a.getAuthority()) || "ROLE_REVIEW_ADMIN".equals(a.getAuthority()));
        if (!r.getUserId().equals(login.getUserId()) && !isAdmin) {
            return Result.fail(403, "无权限");
        }
        int removed = hideReplyTree(id);
        if (removed > 0) {
            ForumTopic t = topicMapper.selectById(r.getTopicId());
            if (t != null) {
                t.setReplyCount(Math.max(0, (t.getReplyCount() == null ? 0 : t.getReplyCount()) - removed));
                topicMapper.updateById(t);
            }
        }
        return Result.ok();
    }

    private int hideReplyTree(Integer rootId) {
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(rootId);
        int changed = 0;
        Integer topicId = null;
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            ForumReply cur = replyMapper.selectById(id);
            if (cur == null) continue;
            if (topicId == null) topicId = cur.getTopicId();
            if (topicId != null && !topicId.equals(cur.getTopicId())) continue;
            if (cur.getStatus() != null && cur.getStatus() == 1) {
                cur.setStatus(0);
                replyMapper.updateById(cur);
                changed++;
            }
            List<ForumReply> children = replyMapper.selectList(new LambdaQueryWrapper<ForumReply>()
                    .eq(ForumReply::getTopicId, cur.getTopicId()).eq(ForumReply::getParentId, cur.getId()));
            for (ForumReply c : children) queue.offer(c.getId());
        }
        return changed;
    }
}
