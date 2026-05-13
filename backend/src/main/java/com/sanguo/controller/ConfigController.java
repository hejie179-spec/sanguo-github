package com.sanguo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.common.Result;
import com.sanguo.entity.SystemConfig;
import com.sanguo.mapper.SystemConfigMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "系统配置-公开")
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Resource
    private SystemConfigMapper configMapper;

    @ApiOperation("站点配置-前台用")
    @GetMapping("/site")
    public Result<Map<String, String>> site() {
        List<SystemConfig> list = configMapper.selectList(null);
        Map<String, String> data = new HashMap<>();
        for (SystemConfig c : list) {
            if (c.getConfigKey() != null && c.getConfigValue() != null) {
                data.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        return Result.ok(data);
    }
}
