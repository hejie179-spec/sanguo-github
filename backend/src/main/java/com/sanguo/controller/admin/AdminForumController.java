package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.ForumReply;
import com.sanguo.entity.ForumTopic;
import com.sanguo.mapper.ForumReplyMapper;
import com.sanguo.mapper.ForumTopicMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "管理-论坛")
@RestController
@RequestMapping("/admin/forum")
public class AdminForumController {

    @Resource
    private ForumTopicMapper topicMapper;
    @Resource
    private ForumReplyMapper replyMapper;

    @ApiOperation("主题列表")
    @GetMapping("/topic/list")
    public Result<PageResult<ForumTopic>> topicList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ForumTopic> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(ForumTopic::getStatus, status);
        q.orderByDesc(ForumTopic::getCreateTime);
        Page<ForumTopic> page = topicMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("置顶/取消置顶")
    @PutMapping("/topic/top/{id}")
    public Result<Void> setTop(@PathVariable Integer id, @RequestParam Integer topStatus) {
        ForumTopic t = topicMapper.selectById(id);
        if (t == null) return Result.fail("主题不存在");
        t.setTopStatus(topStatus);
        topicMapper.updateById(t);
        return Result.ok();
    }

    @ApiOperation("关闭主题")
    @PutMapping("/topic/close/{id}")
    public Result<Void> close(@PathVariable Integer id) {
        ForumTopic t = topicMapper.selectById(id);
        if (t == null) return Result.fail("主题不存在");
        t.setStatus(Constants.TOPIC_CLOSED);
        topicMapper.updateById(t);
        return Result.ok();
    }

    @ApiOperation("删除主题")
    @DeleteMapping("/topic/{id}")
    public Result<Void> deleteTopic(@PathVariable Integer id) {
        topicMapper.deleteById(id);
        return Result.ok();
    }

    @ApiOperation("回复列表")
    @GetMapping("/reply/list")
    public Result<PageResult<ForumReply>> replyList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer topicId) {
        LambdaQueryWrapper<ForumReply> q = new LambdaQueryWrapper<>();
        if (topicId != null) q.eq(ForumReply::getTopicId, topicId);
        q.orderByDesc(ForumReply::getCreateTime);
        Page<ForumReply> page = replyMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("删除回复")
    @DeleteMapping("/reply/{id}")
    public Result<Void> deleteReply(@PathVariable Integer id) {
        replyMapper.deleteById(id);
        return Result.ok();
    }
}
