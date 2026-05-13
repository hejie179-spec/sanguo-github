package com.sanguo.ai;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ZhipuClient {

    @Value("${ai.zhipu.api-key}")
    private String apiKey;

    @Value("${ai.zhipu.chat-model:glm-4-flash}")
    private String chatModel;

    @Value("${ai.zhipu.embedding-model:embedding-3}")
    private String embedModel;

    public String chat(String systemMsg, String userMsg) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemMsg != null && !systemMsg.isEmpty()) {
            Map<String, String> sysMap = new HashMap<>();
            sysMap.put("role", "system");
            sysMap.put("content", systemMsg);
            messages.add(sysMap);
        }
        Map<String, String> usrMap = new HashMap<>();
        usrMap.put("role", "user");
        usrMap.put("content", userMsg);
        messages.add(usrMap);
        return chatWithMessages(messages);
    }

    public String chatWithMessages(List<Map<String, String>> messages) {
        String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);

        try {
            String json = JSONUtil.toJsonStr(body);
            String result = HttpUtil.createPost(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .timeout(60000)
                    .execute().body();

            return JSONUtil.parseObj(result)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");
        } catch (Exception e) {
            log.error("调用智谱Chat接口失败", e);
            return "很抱歉，AI 暂时无法响应您的请求，请稍后再试。";
        }
    }

    public List<List<Double>> embed(List<String> texts) {
        String url = "https://open.bigmodel.cn/api/paas/v4/embeddings";
        Map<String, Object> body = new HashMap<>();
        body.put("model", embedModel);
        body.put("input", texts);

        try {
            String json = JSONUtil.toJsonStr(body);
            String result = HttpUtil.createPost(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .timeout(30000)
                    .execute().body();

            JSONObject resObj = JSONUtil.parseObj(result);
            if (!resObj.containsKey("data")) {
                log.error("Embedding 响应异常: {}", result);
                throw new RuntimeException("Embedding 响应异常");
            }
            JSONArray dataArr = resObj.getJSONArray("data");
            List<List<Double>> embeddings = new ArrayList<>();
            for (int i = 0; i < dataArr.size(); i++) {
                JSONArray vecArr = dataArr.getJSONObject(i).getJSONArray("embedding");
                List<Double> vec = new ArrayList<>();
                for (int j = 0; j < vecArr.size(); j++) {
                    vec.add(vecArr.getDouble(j));
                }
                embeddings.add(vec);
            }
            return embeddings;
        } catch (Exception e) {
            log.error("调用智谱Embedding接口失败", e);
            throw new RuntimeException("Embedding failed", e);
        }
    }
}
