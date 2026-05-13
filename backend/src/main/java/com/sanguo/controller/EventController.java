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

@Api(tags = "事件")
@RestController
@RequestMapping("/event")
public class EventController {

    @Resource
    private EventMapper eventMapper;
    @Resource
    private PersonEventMapper personEventMapper;
    @Resource
    private EventAllusionMapper eventAllusionMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private AllusionMapper allusionMapper;

    @ApiOperation("分页列表")
    @GetMapping("/list")
    public Result<PageResult<Event>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Event> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Event::getTitle, keyword).or().like(Event::getContent, keyword));
        }
        if (StringUtils.hasText(dynasty)) q.eq(Event::getDynasty, dynasty);
        if (StringUtils.hasText(type)) q.eq(Event::getType, type);
        q.orderByAsc(Event::getSort).orderByDesc(Event::getId);
        Page<Event> page = eventMapper.selectPage(new Page<>(current, size), q);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @ApiOperation("详情（含关联人物、典故）")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Event event = eventMapper.selectById(id);
        if (event == null) return Result.fail("事件不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("event", event);
        List<PersonEvent> peList = personEventMapper.selectList(new LambdaQueryWrapper<PersonEvent>().eq(PersonEvent::getEventId, id));
        if (!peList.isEmpty()) {
            List<Person> persons = personMapper.selectBatchIds(peList.stream().map(PersonEvent::getPersonId).collect(Collectors.toList()));
            data.put("persons", persons);
        } else {
            data.put("persons", java.util.Collections.emptyList());
        }
        List<EventAllusion> eaList = eventAllusionMapper.selectList(new LambdaQueryWrapper<EventAllusion>().eq(EventAllusion::getEventId, id));
        if (!eaList.isEmpty()) {
            List<Allusion> allusions = allusionMapper.selectBatchIds(eaList.stream().map(EventAllusion::getAllusionId).collect(Collectors.toList()));
            data.put("allusions", allusions);
        } else {
            data.put("allusions", java.util.Collections.emptyList());
        }
        return Result.ok(data);
    }

    @ApiOperation("新增-管理员")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Event entity) {
        eventMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    @ApiOperation("更新-管理员")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Event entity) {
        eventMapper.updateById(entity);
        return Result.ok();
    }

    @ApiOperation("删除-管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        eventMapper.deleteById(id);
        return Result.ok();
    }

    @ApiOperation("设置关联人物-管理员")
    @PostMapping("/person/{eventId}")
    public Result<Void> setPersons(@PathVariable Integer eventId, @RequestBody List<Integer> personIds) {
        personEventMapper.delete(new LambdaQueryWrapper<PersonEvent>().eq(PersonEvent::getEventId, eventId));
        for (Integer pid : personIds) {
            PersonEvent pe = new PersonEvent();
            pe.setPersonId(pid);
            pe.setEventId(eventId);
            personEventMapper.insert(pe);
        }
        return Result.ok();
    }

    @ApiOperation("设置关联典故-管理员")
    @PostMapping("/allusion/{eventId}")
    public Result<Void> setAllusions(@PathVariable Integer eventId, @RequestBody List<Integer> allusionIds) {
        eventAllusionMapper.delete(new LambdaQueryWrapper<EventAllusion>().eq(EventAllusion::getEventId, eventId));
        for (Integer aid : allusionIds) {
            EventAllusion ea = new EventAllusion();
            ea.setEventId(eventId);
            ea.setAllusionId(aid);
            eventAllusionMapper.insert(ea);
        }
        return Result.ok();
    }
}
