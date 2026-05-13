package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Article;
import com.sanguo.entity.ArticleLike;
import com.sanguo.entity.User;
import com.sanguo.mapper.ArticleLikeMapper;
import com.sanguo.mapper.ArticleMapper;
import com.sanguo.mapper.UserMapper;
import com.sanguo.security.LoginUser;
import com.sanguo.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "文章")
@RestController
@RequestMapping("/article")
public class ArticleController {

    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleLikeMapper articleLikeMapper;
    @Resource
    private NotificationService notificationService;

    @ApiOperation("分页列表-仅已通过")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        q.eq(Article::getStatus, Constants.ARTICLE_APPROVED);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getContent, keyword));
        }
        if (StringUtils.hasText(category)) q.eq(Article::getCategory, category);
        q.orderByDesc(Article::getCreateTime);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, size), q);
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Article a : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("userId", a.getUserId());
            m.put("category", a.getCategory());
            m.put("coverUrl", a.getCoverUrl());
            m.put("viewCount", a.getViewCount());
            m.put("likeCount", a.getLikeCount());
            m.put("commentCount", a.getCommentCount());
            m.put("createTime", a.getCreateTime());
            User u = userMapper.selectById(a.getUserId());
            m.put("authorName", u != null ? u.getUsername() : "");
            records.add(m);
        }
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    @ApiOperation("详情-增加阅读量")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Article a = articleMapper.selectById(id);
        if (a == null) return Result.fail("文章不存在");
        if (a.getStatus() == null || a.getStatus() != Constants.ARTICLE_APPROVED) {
            LoginUser login = null;
            try {
                login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            } catch (Exception ignored) {}
            if (login == null || !login.getUserId().equals(a.getUserId())) {
                return Result.fail(403, "无权限查看");
            }
        }
        a.setViewCount((a.getViewCount() == null ? 0 : a.getViewCount()) + 1);
        articleMapper.updateById(a);
        Map<String, Object> data = new HashMap<>();
        data.put("article", a);
        User u = userMapper.selectById(a.getUserId());
        data.put("authorName", u != null ? u.getUsername() : "");
        boolean liked = false;
        try {
            LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            liked = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, login.getUserId())) > 0;
        } catch (Exception ignored) {}
        data.put("liked", liked);
        return Result.ok(data);
    }

    @ApiOperation("我的文章列表")
    @GetMapping("/my")
    public Result<PageResult<Article>> my(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        q.eq(Article::getUserId, login.getUserId());
        if (status != null) q.eq(Article::getStatus, status);
        q.orderByDesc(Article::getCreateTime);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("发布/保存草稿")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Article entity) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setUserId(login.getUserId());
        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : Constants.ARTICLE_PENDING);
        entity.setViewCount(0);
        entity.setLikeCount(0);
        entity.setCommentCount(0);
        if (entity.getId() != null) {
            Article old = articleMapper.selectById(entity.getId());
            if (old == null || !old.getUserId().equals(login.getUserId())) {
                return Result.fail(403, "无权限");
            }
            articleMapper.updateById(entity);
            return Result.ok(entity.getId());
        }
        articleMapper.insert(entity);
        if (entity.getStatus() != null && entity.getStatus() == Constants.ARTICLE_PENDING) {
            notificationService.notifyReviewAdmins("article_submitted",
                    "用户「" + login.getUsername() + "」提交了新文章：「" + entity.getTitle() + "」",
                    "/admin/article");
        }
        return Result.ok(entity.getId());
    }

    @ApiOperation("删除-仅本人未审核/已驳回可删")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Article a = articleMapper.selectById(id);
        if (a == null || !a.getUserId().equals(login.getUserId())) {
            return Result.fail(403, "无权限");
        }
        if (a.getStatus() != null && a.getStatus() == Constants.ARTICLE_APPROVED) {
            return Result.fail("已通过文章不可删除，可联系管理员");
        }
        articleMapper.deleteById(id);
        return Result.ok();
    }

    @ApiOperation("点赞/取消赞")
    @PostMapping("/like/{id}")
    public Result<Boolean> like(@PathVariable Integer id) {
        LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Article a = articleMapper.selectById(id);
        if (a == null || a.getStatus() == null || a.getStatus() != Constants.ARTICLE_APPROVED) {
            return Result.fail("文章不存在或未通过");
        }
        ArticleLike existing = articleLikeMapper.selectOne(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, login.getUserId()));
        boolean liked;
        if (existing != null) {
            articleLikeMapper.deleteById(existing.getId());
            a.setLikeCount(Math.max(0, (a.getLikeCount() == null ? 0 : a.getLikeCount()) - 1));
            liked = false;
        } else {
            ArticleLike al = new ArticleLike();
            al.setArticleId(id);
            al.setUserId(login.getUserId());
            articleLikeMapper.insert(al);
            a.setLikeCount((a.getLikeCount() == null ? 0 : a.getLikeCount()) + 1);
            liked = true;
        }
        articleMapper.updateById(a);
        return Result.ok(liked);
    }
}
