package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.AiQuestionRecord;
import com.sanguo.entity.Allusion;
import com.sanguo.entity.Article;
import com.sanguo.entity.Collection;
import com.sanguo.entity.Event;
import com.sanguo.entity.ForumTopic;
import com.sanguo.entity.Literature;
import com.sanguo.entity.Person;
import com.sanguo.mapper.AiQuestionRecordMapper;
import com.sanguo.mapper.AllusionMapper;
import com.sanguo.mapper.ArticleMapper;
import com.sanguo.mapper.CollectionMapper;
import com.sanguo.mapper.EventMapper;
import com.sanguo.mapper.ForumTopicMapper;
import com.sanguo.mapper.LiteratureMapper;
import com.sanguo.mapper.PersonMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sanguo.security.LoginUser;

@Api(tags = "收藏")
@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Resource
    private CollectionMapper collectionMapper;

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
    private AiQuestionRecordMapper aiQuestionRecordMapper;
    @Resource
    private ForumTopicMapper forumTopicMapper;

    @ApiOperation("收藏/取消")
    @PostMapping("/toggle")
    public Result<Boolean> toggle(@RequestParam Integer targetType, @RequestParam Integer targetId) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<Collection> q = new LambdaQueryWrapper<>();
        q.eq(Collection::getUserId, login.getUserId()).eq(Collection::getTargetType, targetType).eq(Collection::getTargetId, targetId);
        Collection exist = collectionMapper.selectOne(q);
        if (exist != null) {
            collectionMapper.deleteById(exist.getId());
            return Result.ok(false);
        }
        Collection c = new Collection();
        c.setUserId(login.getUserId());
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        collectionMapper.insert(c);
        return Result.ok(true);
    }

    @ApiOperation("是否已收藏")
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam Integer targetType, @RequestParam Integer targetId) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long n = collectionMapper.selectCount(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, login.getUserId()).eq(Collection::getTargetType, targetType).eq(Collection::getTargetId, targetId));
        return Result.ok(n > 0);
    }

    @ApiOperation("我的收藏列表")
    @GetMapping("/my")
    public Result<PageResult<Collection>> my(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer targetType) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<Collection> q = new LambdaQueryWrapper<>();
        q.eq(Collection::getUserId, login.getUserId());
        if (targetType != null) q.eq(Collection::getTargetType, targetType);
        q.orderByDesc(Collection::getCreateTime);
        Page<Collection> page = collectionMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("我的收藏列表-带目标信息")
    @GetMapping("/my-detail")
    public Result<PageResult<Map<String, Object>>> myDetail(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<Collection> q = new LambdaQueryWrapper<>();
        q.eq(Collection::getUserId, login.getUserId());
        if (targetType != null) q.eq(Collection::getTargetType, targetType);
        if (startDate != null && !startDate.isEmpty()) {
            LocalDate d = LocalDate.parse(startDate);
            q.ge(Collection::getCreateTime, LocalDateTime.of(d, LocalTime.MIN));
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDate d = LocalDate.parse(endDate);
            q.le(Collection::getCreateTime, LocalDateTime.of(d, LocalTime.MAX));
        }
        q.orderByDesc(Collection::getCreateTime);
        Page<Collection> page = collectionMapper.selectPage(new Page<>(current, size), q);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Collection c : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("targetType", c.getTargetType());
            m.put("targetId", c.getTargetId());
            m.put("createTime", c.getCreateTime());
            fillTargetInfo(m, c.getTargetType(), c.getTargetId());
            records.add(m);
        }
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    private void fillTargetInfo(Map<String, Object> m, Integer targetType, Integer targetId) {
        if (targetType == null || targetId == null) return;
        if (targetType == Constants.COLLECT_ARTICLE) {
            Article a = articleMapper.selectById(targetId);
            if (a != null) {
                m.put("title", a.getTitle());
                m.put("coverUrl", a.getCoverUrl());
            }
            return;
        }
        if (targetType == Constants.COLLECT_PERSON) {
            Person p = personMapper.selectById(targetId);
            if (p != null) {
                m.put("title", p.getName());
                m.put("coverUrl", p.getImageUrl());
            }
            return;
        }
        if (targetType == Constants.COLLECT_EVENT) {
            Event e = eventMapper.selectById(targetId);
            if (e != null) {
                m.put("title", e.getTitle());
                m.put("coverUrl", e.getImageUrl());
            }
            return;
        }
        if (targetType == Constants.COLLECT_LITERATURE) {
            Literature l = literatureMapper.selectById(targetId);
            if (l != null) {
                m.put("title", l.getTitle());
                m.put("coverUrl", l.getImageUrl());
            }
            return;
        }
        if (targetType == Constants.COLLECT_ALLUSION) {
            Allusion a = allusionMapper.selectById(targetId);
            if (a != null) {
                m.put("title", a.getTitle());
            }
            return;
        }
        if (targetType == Constants.COLLECT_AI_ANSWER) {
            AiQuestionRecord r = aiQuestionRecordMapper.selectById(targetId);
            if (r != null) {
                m.put("title", r.getQuestionContent());
            }
            return;
        }
        if (targetType == Constants.COLLECT_FORUM_TOPIC) {
            ForumTopic t = forumTopicMapper.selectById(targetId);
            if (t != null) {
                m.put("title", t.getTitle());
                m.put("coverUrl", t.getCoverUrl());
            }
        }
    }
}
