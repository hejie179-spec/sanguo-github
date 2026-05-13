package com.sanguo.controller;

import com.sanguo.common.Result;
import com.sanguo.security.LoginUser;
import com.sanguo.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "搜索与推荐")
@RestController
@RequestMapping("/search")
public class SearchController {

    @Resource
    private SearchService searchService;

    @ApiOperation("分类搜索")
    @GetMapping
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(searchService.search(keyword, limit));
    }

    @ApiOperation("全局搜索（按权重排序）")
    @GetMapping("/global")
    public Result<List<Map<String, Object>>> globalSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.ok(searchService.globalSearch(keyword, limit));
    }

    @ApiOperation("个性化推荐（需登录）")
    @GetMapping("/recommend")
    public Result<List<Map<String, Object>>> recommend(
            @RequestParam(defaultValue = "10") Integer limit) {
        Integer userId = null;
        try {
            LoginUser login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userId = login.getUserId();
        } catch (Exception ignored) {}
        return Result.ok(searchService.recommend(userId, limit));
    }
}
