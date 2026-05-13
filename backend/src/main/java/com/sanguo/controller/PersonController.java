package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.common.PageResult;
import com.sanguo.common.Result;
import com.sanguo.entity.*;
import com.sanguo.mapper.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "人物")
@RestController
@RequestMapping("/person")
public class PersonController {

    @Resource
    private PersonMapper personMapper;
    @Resource
    private PersonEventMapper personEventMapper;
    @Resource
    private PersonLiteratureMapper personLiteratureMapper;
    @Resource
    private EventMapper eventMapper;
    @Resource
    private LiteratureMapper literatureMapper;

    @ApiOperation("分页列表")
    @GetMapping("/list")
    public Result<PageResult<Person>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dynasty) {
        LambdaQueryWrapper<Person> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Person::getName, keyword).or().like(Person::getAlias, keyword).or().like(Person::getIntroduction, keyword));
        }
        if (StringUtils.hasText(dynasty)) q.eq(Person::getDynasty, dynasty);
        q.orderByAsc(Person::getSort).orderByDesc(Person::getId);
        Page<Person> page = personMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("详情（含关联事件、史料）")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Person person = personMapper.selectById(id);
        if (person == null) return Result.fail("人物不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("person", person);
        List<PersonEvent> peList = personEventMapper.selectList(new LambdaQueryWrapper<PersonEvent>().eq(PersonEvent::getPersonId, id));
        if (!peList.isEmpty()) {
            List<Event> events = eventMapper.selectBatchIds(peList.stream().map(PersonEvent::getEventId).collect(Collectors.toList()));
            data.put("events", events);
        } else {
            data.put("events", java.util.Collections.emptyList());
        }
        List<PersonLiterature> plList = personLiteratureMapper.selectList(new LambdaQueryWrapper<PersonLiterature>().eq(PersonLiterature::getPersonId, id));
        if (!plList.isEmpty()) {
            List<Literature> literatures = literatureMapper.selectBatchIds(plList.stream().map(PersonLiterature::getLiteratureId).collect(Collectors.toList()));
            data.put("literatures", literatures);
        } else {
            data.put("literatures", java.util.Collections.emptyList());
        }
        return Result.ok(data);
    }

    @ApiOperation("新增-管理员")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Person entity) {
        personMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    @ApiOperation("更新-管理员")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Person entity) {
        personMapper.updateById(entity);
        return Result.ok();
    }

    @ApiOperation("删除-管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        personMapper.deleteById(id);
        return Result.ok();
    }

    @ApiOperation("设置关联事件-管理员")
    @PostMapping("/event/{personId}")
    public Result<Void> setEvents(@PathVariable Integer personId, @RequestBody List<Integer> eventIds) {
        personEventMapper.delete(new LambdaQueryWrapper<PersonEvent>().eq(PersonEvent::getPersonId, personId));
        for (Integer eid : eventIds) {
            PersonEvent pe = new PersonEvent();
            pe.setPersonId(personId);
            pe.setEventId(eid);
            personEventMapper.insert(pe);
        }
        return Result.ok();
    }

    @ApiOperation("设置关联史料-管理员")
    @PostMapping("/literature/{personId}")
    public Result<Void> setLiteratures(@PathVariable Integer personId, @RequestBody List<Integer> literatureIds) {
        personLiteratureMapper.delete(new LambdaQueryWrapper<PersonLiterature>().eq(PersonLiterature::getPersonId, personId));
        for (Integer lid : literatureIds) {
            PersonLiterature pl = new PersonLiterature();
            pl.setPersonId(personId);
            pl.setLiteratureId(lid);
            personLiteratureMapper.insert(pl);
        }
        return Result.ok();
    }
}
