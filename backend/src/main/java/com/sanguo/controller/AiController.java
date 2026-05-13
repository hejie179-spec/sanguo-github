package com.sanguo.controller;

import com.sanguo.common.Result;
import com.sanguo.dto.AiAskRequest;
import com.sanguo.entity.AiQuestionRecord;
import com.sanguo.security.LoginUser;
import com.sanguo.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "AI智能问答")
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;
    /**
     * 获取当前登录用户的ID
     * @return 用户ID，如果未登录则返回null
     */
    private Integer getCurrentUserId() {
        try {
            LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return loginUser.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 向AI提问接口
     * @param params 请求参数，包含questionContent字段
     * @return AI回答结果
     */
    @ApiOperation("向AI提问")
    @PostMapping("/ask")
    public Result<AiQuestionRecord> ask(@RequestBody AiAskRequest params) {
        String question = params == null ? null : params.getQuestionContent();
        if (question == null || question.trim().isEmpty()) {
            return Result.fail("提问内容不能为空");
        }
        // 获取当前登录用户ID
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        // 调用AI服务处理问题并返回结果
        String conversationId = params == null ? null : params.getConversationId();
        Integer seedRecordId = params == null ? null : params.getSeedRecordId();
        Integer maxHistoryTurns = params == null ? null : params.getMaxHistoryTurns();
        AiQuestionRecord record = aiService.ask(userId, question.trim(), conversationId, seedRecordId, maxHistoryTurns);
        return Result.ok(record);
    }

    @ApiOperation("获取提问历史")
    @GetMapping("/history")
    public Result<List<AiQuestionRecord>> history(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.ok(aiService.history(userId, current, size));
    }

    @ApiOperation("获取某个会话的记录")
    @GetMapping("/conversation/records")
    public Result<List<AiQuestionRecord>> conversationRecords(@RequestParam String conversationId) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Result.fail("conversationId不能为空");
        }
        return Result.ok(aiService.conversationRecords(userId, conversationId.trim()));
    }
}
