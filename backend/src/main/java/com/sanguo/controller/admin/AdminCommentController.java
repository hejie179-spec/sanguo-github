package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Article;
import com.sanguo.entity.Comment;
import com.sanguo.mapper.ArticleMapper;
import com.sanguo.mapper.CommentMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Api(tags = "管理-评论")
@RestController
@RequestMapping("/admin/comment")
public class AdminCommentController {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private ArticleMapper articleMapper;

    @ApiOperation("评论列表")
    @GetMapping("/list")
    public Result<PageResult<Comment>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) Integer targetId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        if (targetType != null) q.eq(Comment::getTargetType, targetType);
        if (targetId != null) q.eq(Comment::getTargetId, targetId);
        if (status != null) q.eq(Comment::getStatus, status);
        q.orderByDesc(Comment::getCreateTime);
        Page<Comment> page = commentMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("删除/隐藏")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        hideCommentTree(id);
        return Result.ok();
    }

    @ApiOperation("永久删除(含子回复)")
    @DeleteMapping("/purge/{id}")
    public Result<Void> purge(@PathVariable Integer id) {
        Comment root = commentMapper.selectById(id);
        int removedVisible = deleteCommentTree(id);
        if (removedVisible > 0) {
            if (root != null && root.getTargetType() != null && root.getTargetType() == Constants.TARGET_ARTICLE) {
                Article a = articleMapper.selectById(root.getTargetId());
                if (a != null && a.getCommentCount() != null && a.getCommentCount() > 0) {
                    a.setCommentCount(Math.max(0, a.getCommentCount() - removedVisible));
                    articleMapper.updateById(a);
                }
            }
        }
        return Result.ok();
    }

    private void hideCommentTree(Integer rootId) {
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(rootId);
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            Comment cur = commentMapper.selectById(id);
            if (cur == null) continue;
            if (cur.getStatus() == null || cur.getStatus() != 0) {
                cur.setStatus(0);
                commentMapper.updateById(cur);
            }
            List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
            for (Comment child : children) queue.offer(child.getId());
        }
    }

    private int deleteCommentTree(Integer rootId) {
        Comment root = commentMapper.selectById(rootId);
        if (root == null) return 0;
        Integer targetType = root.getTargetType();
        Integer targetId = root.getTargetId();

        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(rootId);
        List<Integer> ids = new ArrayList<>();
        int visibleCount = 0;
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            Comment cur = commentMapper.selectById(id);
            if (cur == null) continue;
            if (targetType != null && targetId != null) {
                if (!targetType.equals(cur.getTargetType()) || !targetId.equals(cur.getTargetId())) continue;
            }
            ids.add(id);
            if (cur.getStatus() != null && cur.getStatus() == 1) visibleCount++;
            List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
            for (Comment child : children) queue.offer(child.getId());
        }
        for (Integer id : ids) commentMapper.deleteById(id);
        return visibleCount;
    }
}
