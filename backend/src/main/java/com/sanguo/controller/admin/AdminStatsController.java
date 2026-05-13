package com.sanguo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.common.Constants;
import com.sanguo.common.Result;
import com.sanguo.entity.Article;
import com.sanguo.mapper.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "管理-数据统计")
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Resource
    private com.sanguo.mapper.UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private EventMapper eventMapper;
    @Resource
    private LiteratureMapper literatureMapper;
    @Resource
    private AllusionMapper allusionMapper;
    @Resource
    private ForumTopicMapper forumTopicMapper;

    @ApiOperation("概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("articleCount", articleMapper.selectCount(null));
        data.put("articlePendingCount", articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, Constants.ARTICLE_PENDING)));
        data.put("personCount", personMapper.selectCount(null));
        data.put("eventCount", eventMapper.selectCount(null));
        data.put("literatureCount", literatureMapper.selectCount(null));
        data.put("allusionCount", allusionMapper.selectCount(null));
        data.put("topicCount", forumTopicMapper.selectCount(null));
        return Result.ok(data);
    }
}
