package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.common.Result;
import com.sanguo.entity.SystemConfig;
import com.sanguo.mapper.SystemConfigMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "管理-系统配置")
@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    @Resource
    private SystemConfigMapper configMapper;

    @ApiOperation("配置列表")
    @GetMapping("/list")
    public Result<List<SystemConfig>> list() {
        return Result.ok(configMapper.selectList(null));
    }

    @ApiOperation("保存配置")
    @PutMapping("/save")
    public Result<Void> save(@RequestBody List<SystemConfig> list) {
        for (SystemConfig c : list) {
            if (c.getConfigKey() == null) continue;
            SystemConfig exist = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, c.getConfigKey()));
            if (exist != null) {
                exist.setConfigValue(c.getConfigValue());
                exist.setDescription(c.getDescription());
                configMapper.updateById(exist);
            } else {
                configMapper.insert(c);
            }
        }
        return Result.ok();
    }
}
