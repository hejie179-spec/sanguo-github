package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.Literature;
import com.sanguo.mapper.LiteratureMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "史料")
@RestController
@RequestMapping("/literature")
public class LiteratureController {

    @Resource
    private LiteratureMapper literatureMapper;

    @ApiOperation("分页列表")
    @GetMapping("/list")
    public Result<PageResult<Literature>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Literature> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Literature::getTitle, keyword).or().like(Literature::getAuthor, keyword));
        }
        if (StringUtils.hasText(category)) q.eq(Literature::getCategory, category);
        q.orderByAsc(Literature::getSort).orderByDesc(Literature::getId);
        Page<Literature> page = literatureMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("详情")
    @GetMapping("/detail/{id}")
    public Result<Literature> detail(@PathVariable Integer id) {
        Literature lit = literatureMapper.selectById(id);
        if (lit == null) return Result.fail("史料不存在");
        return Result.ok(lit);
    }

    @ApiOperation("新增-管理员")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Literature entity) {
        literatureMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    @ApiOperation("更新-管理员")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Literature entity) {
        literatureMapper.updateById(entity);
        return Result.ok();
    }

    @ApiOperation("删除-管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        literatureMapper.deleteById(id);
        return Result.ok();
    }
}
