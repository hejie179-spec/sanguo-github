package com.sanguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanguo.ai.VectorStore;
import com.sanguo.ai.ZhipuClient;
import com.sanguo.entity.AiQuestionRecord;
import com.sanguo.entity.Event;
import com.sanguo.entity.Person;
import com.sanguo.mapper.AiQuestionRecordMapper;
import com.sanguo.mapper.EventMapper;
import com.sanguo.mapper.PersonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AiService {

    /**
     * AI 问答核心服务
     *
     * <p>整体策略（按成本/速度从低到高）：</p>
     * <ol>
     *   <li>先尝试“数据库可直接回答”的问题（例如：人物属于哪一国/势力，事件属于哪个时期）</li>
     *   <li>如果 DB 无法命中，再走知识库检索（VectorStore）拼接参考资料</li>
     *   <li>最后调用大模型（ZhipuClient）生成回答，并将问答记录落库</li>
     * </ol>
     *
     * <p>提示：VectorStore 在应用启动后会异步初始化，初始化期间可能提示“暂无命中资料”。</p>
     */
    private static final Pattern ENTITY_BEFORE_BELONGS = Pattern.compile("([\\u4e00-\\u9fa5]{1,10})\\s*属于");
    private static final Pattern ENTITY_BEFORE_IS = Pattern.compile("^\\s*([\\u4e00-\\u9fa5]{1,10})\\s*是");

    @Resource
    private ZhipuClient zhipuClient;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private AiQuestionRecordMapper aiQuestionRecordMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private EventMapper eventMapper;

    public AiQuestionRecord ask(Integer userId, String question) {
        return ask(userId, question, null, null, null);
    }

    public AiQuestionRecord ask(Integer userId, String question, String conversationId, Integer seedRecordId, Integer maxHistoryTurns) {
        String q = question == null ? null : question.trim();
        if (q == null || q.isEmpty()) {
            throw new IllegalArgumentException("question empty");
        }

        String convId = (conversationId == null || conversationId.trim().isEmpty()) ? null : conversationId.trim();
        int turns = (maxHistoryTurns == null || maxHistoryTurns <= 0) ? 6 : Math.min(maxHistoryTurns, 20);

        List<AiQuestionRecord> historyRecords = new ArrayList<>();
        if (convId != null) {
            LambdaQueryWrapper<AiQuestionRecord> hq = new LambdaQueryWrapper<>();
            hq.eq(AiQuestionRecord::getUserId, userId);
            hq.eq(AiQuestionRecord::getConversationId, convId);
            hq.orderByDesc(AiQuestionRecord::getTurnNo);
            hq.last("limit " + turns);
            historyRecords = aiQuestionRecordMapper.selectList(hq);
        }

        AiQuestionRecord seed = null;
        if (convId == null && seedRecordId != null) {
            seed = aiQuestionRecordMapper.selectById(seedRecordId);
            if (seed != null && !userId.equals(seed.getUserId())) {
                seed = null;
            }
        }

        if (convId == null) {
            convId = UUID.randomUUID().toString().replace("-", "");
        }

        // 1) 根据数据库快速回答
        String answer = tryAnswerDynastyFromDb(q);
        if (answer == null || answer.trim().isEmpty()) {
            // 2) 从知识库检索出与问题最相关的若干段文本作为“参考资料”
            List<String> contexts = vectorStore.search(q, 3);

            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是一个专业的汉末三国文化答疑助手。请基于【参考资料】回答问题，避免编造。");
            systemPrompt.append("\n");
            systemPrompt.append("涉及“属于哪个朝代/时期/是否属于三国时期”时，默认采用：东汉末年≈184-220；三国时期≈220（魏建立）-280（晋灭吴）；西晋≈265-316。请给出最准确的归属，必要时可补充“广义三国故事范畴”的说法，但必须明确区分。");
            systemPrompt.append("\n");
            systemPrompt.append("如果参考资料不足，请明确说明不确定，并给出可核查的线索。");
            systemPrompt.append("\n\n");
            systemPrompt.append("【参考资料】\n");
            if (contexts == null || contexts.isEmpty()) {
                systemPrompt.append("（暂无命中资料：知识库初始化中或未检索到）\n");
            } else {
                for (int i = 0; i < contexts.size(); i++) {
                    systemPrompt.append(i + 1).append(". ").append(contexts.get(i)).append("\n");
                }
            }

        // ... existing code ...
    // 3) 调大模型生成回答（成本最高）
            // 构建对话消息列表，用于发送给大模型
            List<Map<String, String>> messages = new ArrayList<>();

            // 添加系统提示词，设定AI助手的角色和行为准则
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt.toString());
            messages.add(sys);

            // 如果存在种子问题（初始问题），将其作为第一条用户消息加入
            if (seed != null) {
                // 添加种子问题的内容
                Map<String, String> u = new HashMap<>();
                u.put("role", "user");
                u.put("content", seed.getQuestionContent());
                messages.add(u);

                // 如果种子问题有对应的回答，也添加到对话历史中
                if (seed.getAnswerContent() != null && !seed.getAnswerContent().trim().isEmpty()) {
                    Map<String, String> a = new HashMap<>();
                    a.put("role", "assistant");
                    a.put("content", seed.getAnswerContent());
                    messages.add(a);
                }
            }

            // 添加历史对话记录，按时间倒序遍历以保持对话顺序
            if (historyRecords != null && !historyRecords.isEmpty()) {
                for (int i = historyRecords.size() - 1; i >= 0; i--) {
                    AiQuestionRecord r = historyRecords.get(i);

                    // 添加历史问题
                    Map<String, String> u = new HashMap<>();
                    u.put("role", "user");
                    u.put("content", r.getQuestionContent());
                    messages.add(u);

                    // 如果历史记录中有回答，也一并添加
                    if (r.getAnswerContent() != null && !r.getAnswerContent().trim().isEmpty()) {
                        Map<String, String> a = new HashMap<>();
                        a.put("role", "assistant");
                        a.put("content", r.getAnswerContent());
                        messages.add(a);
                    }
                }
            }

            // 添加当前用户的问题到消息列表末尾
            Map<String, String> usr = new HashMap<>();
            usr.put("role", "user");
            usr.put("content", q);
            messages.add(usr);

            // 调用智谱AI客户端发送完整的对话消息并获取回答
            answer = zhipuClient.chatWithMessages(messages);
        }
// ... existing code ...


        // 4) 落库：记录用户提问与回答（用于“历史记录”）
        AiQuestionRecord record = new AiQuestionRecord();
        record.setUserId(userId);
        record.setConversationId(convId);
        record.setTurnNo(nextTurnNo(userId, convId));
        record.setQuestionContent(q);
        record.setQuestionType(1);
        record.setAnswerContent(answer);
        aiQuestionRecordMapper.insert(record);
        return record;
    }

    private Integer nextTurnNo(Integer userId, String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) return 1;
        LambdaQueryWrapper<AiQuestionRecord> q = new LambdaQueryWrapper<>();
        q.eq(AiQuestionRecord::getUserId, userId);
        q.eq(AiQuestionRecord::getConversationId, conversationId);
        q.orderByDesc(AiQuestionRecord::getTurnNo);
        q.last("limit 1");
        AiQuestionRecord last = aiQuestionRecordMapper.selectOne(q);
        if (last == null || last.getTurnNo() == null) return 1;
        return last.getTurnNo() + 1;
    }

    private String tryAnswerDynastyFromDb(String question) {
        if (question == null) return null;
        String q = question.trim();
        if (q.isEmpty()) return null;

        boolean timeQuestion = q.contains("朝代") || q.contains("时期") || q.contains("时代") || q.contains("三国时期") || q.contains("三国时代");
        boolean factionQuestion = q.contains("哪一国") || q.contains("哪个国家") || q.contains("势力") || q.contains("阵营") || q.contains("魏") || q.contains("蜀") || q.contains("吴");
        if (!timeQuestion && !factionQuestion) return null;

        String entity = extractEntity(q);
        if (entity == null || entity.isEmpty()) return null;

        Person person = personMapper.selectOne(new LambdaQueryWrapper<Person>()
                .eq(Person::getName, entity)
                .or()
                .eq(Person::getAlias, entity)
                .last("limit 1"));
        if (person != null) {
            if (timeQuestion) return null;
            return buildFactionAnswer(person.getName(), person.getDynasty());
        }

        Event event = eventMapper.selectOne(new LambdaQueryWrapper<Event>()
                .eq(Event::getTitle, entity)
                .last("limit 1"));
        if (event != null) {
            return buildDynastyAnswer(event.getTitle(), event.getDynasty(), "事件");
        }

        return null;
    }

    private String extractEntity(String question) {
        Matcher m = ENTITY_BEFORE_BELONGS.matcher(question);
        if (m.find()) return m.group(1);
        m = ENTITY_BEFORE_IS.matcher(question);
        if (m.find()) return m.group(1);
        return null;
    }

    private String buildFactionAnswer(String name, String dynasty) {
        String d = dynasty == null ? "" : dynasty.trim();
        if (d.isEmpty()) {
            return "人物“" + name + "”在数据库中未标注归属，暂时无法给出确定结论。";
        }
        if ("魏国".equals(d) || "蜀国".equals(d) || "吴国".equals(d)) {
            return "人物“" + name + "”在本系统的归属为" + d + "（魏蜀吴势力划分）。";
        }
        if ("东汉".equals(d) || "西晋".equals(d)) {
            return "人物“" + name + "”在本系统的归属标注为" + d + "（更偏向朝代/时期划分）。";
        }
        return "人物“" + name + "”在本系统的归属标注为“" + d + "”。";
    }

    private String buildDynastyAnswer(String name, String dynasty, String type) {
        String d = dynasty == null ? "" : dynasty.trim();
        if (d.isEmpty()) {
            return type + "“" + name + "”在数据库中未标注朝代/时期，暂时无法给出确定归属。";
        }
        if ("东汉".equals(d)) {
            return type + "“" + name + "”更准确属于东汉末年（汉末，约184-220），不严格属于“三国时期”（通常指220-280）。很多“三国故事”会把汉末到三国合在一起讲，但在时间划分上需要区分。";
        }
        if ("魏国".equals(d) || "蜀国".equals(d) || "吴国".equals(d)) {
            return type + "“" + name + "”属于三国时期，具体归属为" + d + "（通常三国时期指220-280）。";
        }
        if ("西晋".equals(d)) {
            return type + "“" + name + "”属于西晋（约265-316），不属于“三国时期”（220-280）。";
        }
        return type + "“" + name + "”在数据库标注为“" + d + "”。如果你问的是严格时间划分，请以具体年份与政权建立/灭亡节点为准。";
    }

    public List<AiQuestionRecord> history(Integer userId, Long current, Long size) {
        LambdaQueryWrapper<AiQuestionRecord> q = new LambdaQueryWrapper<>();
        q.eq(AiQuestionRecord::getUserId, userId);
        q.orderByDesc(AiQuestionRecord::getCreateTime);
        Page<AiQuestionRecord> page = aiQuestionRecordMapper.selectPage(new Page<>(current, size), q);
        List<AiQuestionRecord> records = page.getRecords();
        if (records != null) {
            for (AiQuestionRecord r : records) {
                if (r.getConversationId() == null || r.getConversationId().trim().isEmpty()) {
                    r.setConversationId("legacy-" + r.getId());
                }
                if (r.getTurnNo() == null) {
                    r.setTurnNo(1);
                }
            }
        }
        return records;
    }

    public List<AiQuestionRecord> conversationRecords(Integer userId, String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) return new ArrayList<>();
        if (conversationId.startsWith("legacy-")) return new ArrayList<>();
        LambdaQueryWrapper<AiQuestionRecord> q = new LambdaQueryWrapper<>();
        q.eq(AiQuestionRecord::getUserId, userId);
        q.eq(AiQuestionRecord::getConversationId, conversationId.trim());
        q.orderByAsc(AiQuestionRecord::getTurnNo);
        q.orderByAsc(AiQuestionRecord::getCreateTime);
        return aiQuestionRecordMapper.selectList(q);
    }
}
