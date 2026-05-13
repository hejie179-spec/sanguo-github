package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.Constants;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Article;
import com.sanguo.entity.User;
import com.sanguo.mapper.ArticleMapper;
import com.sanguo.mapper.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "管理-文章审核")
@RestController
@RequestMapping("/admin/article")
public class AdminArticleController {

    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserMapper userMapper;

    @ApiOperation("文章分页-全部状态")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(Article::getStatus, status);
        if (StringUtils.hasText(category)) q.eq(Article::getCategory, category);
        q.orderByDesc(Article::getCreateTime);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, size), q);
        List<Integer> userIds = page.getRecords().stream().map(Article::getUserId).distinct().collect(Collectors.toList());
        Map<Integer, String> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u.getUsername());
            }
        }
        List<Map<String, Object>> records = page.getRecords().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("userId", a.getUserId());
            m.put("authorName", userMap.getOrDefault(a.getUserId(), ""));
            m.put("category", a.getCategory());
            m.put("status", a.getStatus());
            m.put("reason", a.getReason());
            m.put("viewCount", a.getViewCount());
            m.put("createTime", a.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records));
    }

    @ApiOperation("审核通过")
    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Integer id) {
        Article a = articleMapper.selectById(id);
        if (a == null) return Result.fail("文章不存在");
        a.setStatus(Constants.ARTICLE_APPROVED);
        a.setReason(null);
        articleMapper.updateById(a);
        return Result.ok();
    }

    @ApiOperation("驳回")
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Article a = articleMapper.selectById(id);
        if (a == null) return Result.fail("文章不存在");
        a.setStatus(Constants.ARTICLE_REJECTED);
        a.setReason(body.get("reason"));
        articleMapper.updateById(a);
        return Result.ok();
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        articleMapper.deleteById(id);
        return Result.ok();
    }
}
