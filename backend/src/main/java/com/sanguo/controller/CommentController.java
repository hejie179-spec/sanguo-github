package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.BizException;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Allusion;
import com.sanguo.entity.Article;
import com.sanguo.entity.Comment;
import com.sanguo.entity.Event;
import com.sanguo.entity.ForumReply;
import com.sanguo.entity.ForumTopic;
import com.sanguo.entity.Literature;
import com.sanguo.entity.Person;
import com.sanguo.entity.User;
import com.sanguo.mapper.AllusionMapper;
import com.sanguo.mapper.ArticleMapper;
import com.sanguo.mapper.CommentMapper;
import com.sanguo.mapper.EventMapper;
import com.sanguo.mapper.ForumReplyMapper;
import com.sanguo.mapper.ForumTopicMapper;
import com.sanguo.mapper.LiteratureMapper;
import com.sanguo.mapper.PersonMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.security.LoginUser;
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
import java.util.Comparator;
import java.util.stream.Collectors;

@Api(tags = "评论")
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private EventMapper eventMapper;
    @Resource
    private LiteratureMapper literatureMapper;
    @Resource
    private AllusionMapper allusionMapper;
    @Resource
    private ForumReplyMapper forumReplyMapper;
    @Resource
    private ForumTopicMapper forumTopicMapper;

    private static final int COMMENT_ARTICLE = 1;
    private static final int COMMENT_PERSON = 2;
    private static final int COMMENT_EVENT = 3;
    private static final int COMMENT_LITERATURE = 4;
    private static final int COMMENT_ALLUSION = 5;

    @ApiOperation("评论列表-树形")
    @GetMapping("/list/{targetType}/{targetId}")
    public Result<List<Map<String, Object>>> list(@PathVariable Integer targetType, @PathVariable Integer targetId) {
        validateTarget(targetType, targetId);
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        q.eq(Comment::getTargetType, targetType).eq(Comment::getTargetId, targetId)
                .eq(Comment::getStatus, 1).eq(Comment::getParentId, 0)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> roots = commentMapper.selectList(q);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Comment c : roots) {
            list.add(buildCommentNode(c, targetType, targetId));
        }
        return Result.ok(list);
    }

    private Map<String, Object> buildCommentNode(Comment c, int targetType, int targetId) {
        Map<String, Object> m = new HashMap<>();
        User u = userMapper.selectById(c.getUserId());
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("username", u != null ? u.getUsername() : "");
        m.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
        m.put("content", c.getContent());
        m.put("createTime", c.getCreateTime());
        List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, targetType).eq(Comment::getTargetId, targetId)
                .eq(Comment::getStatus, 1).eq(Comment::getParentId, c.getId())
                .orderByAsc(Comment::getCreateTime));
        m.put("children", children.stream().map(child -> buildCommentNode(child, targetType, targetId)).collect(Collectors.toList()));
        return m;
    }

    @ApiOperation("发表评论")
    @PostMapping("/add")
    public Result<Comment> add(@RequestBody Comment comment) {
        if (comment == null) throw new BizException("参数错误");
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        validateTarget(comment.getTargetType(), comment.getTargetId());
        if (!StringUtils.hasText(comment.getContent()) || comment.getContent().trim().isEmpty()) {
            throw new BizException("评论内容不能为空");
        }
        if (comment.getContent().length() > 500) {
            throw new BizException("评论内容不能超过500字");
        }
        comment.setUserId(login.getUserId());
        comment.setParentId(comment.getParentId() != null ? comment.getParentId() : 0);
        comment.setStatus(1);
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent == null || parent.getStatus() == null || parent.getStatus() != 1) {
                throw new BizException("回复的评论不存在");
            }
            if (!parent.getTargetType().equals(comment.getTargetType()) || !parent.getTargetId().equals(comment.getTargetId())) {
                throw new BizException("评论目标不一致");
            }
        }
        commentMapper.insert(comment);
        if (COMMENT_ARTICLE == comment.getTargetType()) {
            Article a = articleMapper.selectById(comment.getTargetId());
            if (a != null) {
                a.setCommentCount((a.getCommentCount() == null ? 0 : a.getCommentCount()) + 1);
                articleMapper.updateById(a);
            }
        }
        return Result.ok(comment);
    }

    @ApiOperation("删除评论-本人或管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        Comment c = commentMapper.selectById(id);
        if (c == null) return Result.fail("评论不存在");
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = login.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!c.getUserId().equals(login.getUserId()) && !isAdmin) {
            return Result.fail(403, "无权限");
        }
        int removed = hideCommentTree(id);
        if (COMMENT_ARTICLE == c.getTargetType() && removed > 0) {
            Article a = articleMapper.selectById(c.getTargetId());
            if (a != null && a.getCommentCount() != null && a.getCommentCount() > 0) {
                a.setCommentCount(Math.max(0, a.getCommentCount() - removed));
                articleMapper.updateById(a);
            }
        }
        return Result.ok();
    }

    @ApiOperation("我的评论-分页")
    @GetMapping("/my")
    public Result<PageResult<Map<String, Object>>> my(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        q.eq(Comment::getUserId, login.getUserId());
        q.eq(Comment::getStatus, 1);
        q.orderByDesc(Comment::getCreateTime);
        Page<Comment> page = commentMapper.selectPage(new Page<>(current, size), q);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Comment c : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("targetType", c.getTargetType());
            m.put("targetId", c.getTargetId());
            m.put("content", c.getContent());
            m.put("parentId", c.getParentId());
            m.put("status", c.getStatus());
            m.put("createTime", c.getCreateTime());
            fillTargetInfo(m, c.getTargetType(), c.getTargetId());
            if (c.getParentId() != null && c.getParentId() != 0) {
                Comment parent = commentMapper.selectById(c.getParentId());
                if (parent != null) {
                    m.put("parentContent", parent.getContent());
                    User pu = userMapper.selectById(parent.getUserId());
                    m.put("parentUsername", pu != null ? pu.getUsername() : "");
                }
            }
            records.add(m);
        }
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    @ApiOperation("我的评论-包含论坛回复")
    @GetMapping("/my-all")
    public Result<PageResult<Map<String, Object>>> myAll(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long totalComment = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, login.getUserId())
                .eq(Comment::getStatus, 1));
        long totalForum = forumReplyMapper.selectCount(new LambdaQueryWrapper<ForumReply>()
                .eq(ForumReply::getUserId, login.getUserId())
                .eq(ForumReply::getStatus, 1));
        long total = totalComment + totalForum;

        long take = Math.max(1, current) * Math.max(1, size);
        List<Map<String, Object>> merged = new ArrayList<>();

        List<Comment> comments = commentMapper.selectPage(new Page<>(1, take),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, login.getUserId())
                        .eq(Comment::getStatus, 1)
                        .orderByDesc(Comment::getCreateTime)).getRecords();
        for (Comment c : comments) merged.add(buildMyCommentItem(c));

        List<ForumReply> replies = forumReplyMapper.selectPage(new Page<>(1, take),
                new LambdaQueryWrapper<ForumReply>()
                        .eq(ForumReply::getUserId, login.getUserId())
                        .eq(ForumReply::getStatus, 1)
                        .orderByDesc(ForumReply::getCreateTime)).getRecords();
        for (ForumReply r : replies) merged.add(buildMyForumReplyItem(r));

        merged.sort((a, b) -> {
            java.time.LocalDateTime ta = (java.time.LocalDateTime) a.get("createTime");
            java.time.LocalDateTime tb = (java.time.LocalDateTime) b.get("createTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        int fromIndex = (int) ((Math.max(1, current) - 1) * Math.max(1, size));
        int toIndex = (int) Math.min(merged.size(), fromIndex + Math.max(1, size));
        List<Map<String, Object>> pageRecords = new ArrayList<>();
        if (fromIndex < merged.size()) pageRecords = merged.subList(fromIndex, toIndex);

        return Result.ok(PageResult.of(total, current, size, pageRecords));
    }

    private Map<String, Object> buildMyCommentItem(Comment c) {
        Map<String, Object> m = new HashMap<>();
        m.put("kind", "comment");
        m.put("id", c.getId());
        m.put("targetType", c.getTargetType());
        m.put("targetId", c.getTargetId());
        m.put("content", c.getContent());
        m.put("parentId", c.getParentId());
        m.put("status", c.getStatus());
        m.put("createTime", c.getCreateTime());
        fillTargetInfo(m, c.getTargetType(), c.getTargetId());
        if (c.getParentId() != null && c.getParentId() != 0) {
            Comment parent = commentMapper.selectById(c.getParentId());
            if (parent != null) {
                m.put("parentContent", parent.getContent());
                User pu = userMapper.selectById(parent.getUserId());
                m.put("parentUsername", pu != null ? pu.getUsername() : "");
            }
        }
        return m;
    }

    private Map<String, Object> buildMyForumReplyItem(ForumReply r) {
        Map<String, Object> m = new HashMap<>();
        m.put("kind", "forumReply");
        m.put("id", r.getId());
        m.put("topicId", r.getTopicId());
        m.put("content", r.getContent());
        m.put("parentId", r.getParentId());
        m.put("status", r.getStatus());
        m.put("createTime", r.getCreateTime());
        ForumTopic t = forumTopicMapper.selectById(r.getTopicId());
        if (t != null) {
            m.put("targetTitle", t.getTitle());
            m.put("targetCoverUrl", t.getCoverUrl());
        }
        if (r.getParentId() != null && r.getParentId() != 0) {
            ForumReply parent = forumReplyMapper.selectById(r.getParentId());
            if (parent != null) {
                m.put("parentContent", parent.getContent());
                User pu = userMapper.selectById(parent.getUserId());
                m.put("parentUsername", pu != null ? pu.getUsername() : "");
            }
        }
        return m;
    }

    private void fillTargetInfo(Map<String, Object> m, Integer targetType, Integer targetId) {
        if (targetType == null || targetId == null) return;
        if (targetType == COMMENT_ARTICLE) {
            Article a = articleMapper.selectById(targetId);
            if (a != null) {
                m.put("targetTitle", a.getTitle());
                m.put("targetCoverUrl", a.getCoverUrl());
            }
            return;
        }
        if (targetType == COMMENT_PERSON) {
            Person p = personMapper.selectById(targetId);
            if (p != null) {
                m.put("targetTitle", p.getName());
                m.put("targetCoverUrl", p.getImageUrl());
            }
            return;
        }
        if (targetType == COMMENT_EVENT) {
            Event e = eventMapper.selectById(targetId);
            if (e != null) {
                m.put("targetTitle", e.getTitle());
                m.put("targetCoverUrl", e.getImageUrl());
            }
            return;
        }
        if (targetType == COMMENT_LITERATURE) {
            Literature l = literatureMapper.selectById(targetId);
            if (l != null) {
                m.put("targetTitle", l.getTitle());
                m.put("targetCoverUrl", l.getImageUrl());
            }
            return;
        }
        if (targetType == COMMENT_ALLUSION) {
            Allusion a = allusionMapper.selectById(targetId);
            if (a != null) {
                m.put("targetTitle", a.getTitle());
            }
        }
    }

    private int hideCommentTree(Integer rootId) {
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(rootId);
        int changed = 0;
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            Comment cur = commentMapper.selectById(id);
            if (cur == null) continue;
            if (cur.getStatus() != null && cur.getStatus() == 1) {
                cur.setStatus(0);
                commentMapper.updateById(cur);
                changed++;
            }
            List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
            for (Comment child : children) queue.offer(child.getId());
        }
        return changed;
    }

    private void validateTarget(Integer targetType, Integer targetId) {
        if (targetType == null || targetId == null) throw new BizException("参数错误");
        if (targetId <= 0) throw new BizException("参数错误");
        if (targetType != COMMENT_ARTICLE && targetType != COMMENT_PERSON && targetType != COMMENT_EVENT
                && targetType != COMMENT_LITERATURE && targetType != COMMENT_ALLUSION) {
            throw new BizException("参数错误");
        }
        if (targetType == COMMENT_ARTICLE) {
            Article a = articleMapper.selectById(targetId);
            if (a == null) throw new BizException("文章不存在");
        }
    }
}
