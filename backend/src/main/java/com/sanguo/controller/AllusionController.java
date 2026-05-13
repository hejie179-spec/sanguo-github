package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Allusion;
import com.sanguo.mapper.AllusionMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "典故")
@RestController
@RequestMapping("/allusion")
public class AllusionController {

    @Resource
    private AllusionMapper allusionMapper;

    @ApiOperation("分页列表")
    @GetMapping("/list")
    public Result<PageResult<Allusion>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Allusion> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Allusion::getTitle, keyword).or().like(Allusion::getSource, keyword));
        }
        q.orderByAsc(Allusion::getSort).orderByDesc(Allusion::getId);
        Page<Allusion> page = allusionMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("详情")
    @GetMapping("/detail/{id}")
    public Result<Allusion> detail(@PathVariable Integer id) {
        Allusion a = allusionMapper.selectById(id);
        if (a == null) return Result.fail("典故不存在");
        return Result.ok(a);
    }

    @ApiOperation("新增-管理员")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Allusion entity) {
        allusionMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    @ApiOperation("更新-管理员")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Allusion entity) {
        allusionMapper.updateById(entity);
        return Result.ok();
    }

    @ApiOperation("删除-管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        allusionMapper.deleteById(id);
        return Result.ok();
    }
}
