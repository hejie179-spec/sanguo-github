package com.sanguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sanguo.common.Constants;
import com.sanguo.entity.*;
import com.sanguo.mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SearchService {

    @Resource
    private PersonMapper personMapper;
    @Resource
    private EventMapper eventMapper;
    @Resource
    private LiteratureMapper literatureMapper;
    @Resource
    private AllusionMapper allusionMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ForumTopicMapper forumTopicMapper;
    @Resource
    private CollectionMapper collectionMapper;

    /**
     * 统一搜索接口
     * @param keyword 搜索关键词
     * @param limit 每类返回条数
     * @return 分类搜索结果
     */
    public Map<String, Object> search(String keyword, int limit) {
        String kw = keyword != null ? keyword.trim() : "";
        Map<String, Object> result = new HashMap<>();
        if (kw.isEmpty()) return result;

        result.put("person", searchPerson(kw, limit));
        result.put("event", searchEvent(kw, limit));
        result.put("literature", searchLiterature(kw, limit));
        result.put("allusion", searchAllusion(kw, limit));
        result.put("article", searchArticle(kw, limit));

        return result;
    }

    /**
     * 全局搜索：返回所有匹配结果并按权重排序
     * @param keyword 搜索关键词
     * @param limit 最大返回条数
     * @return 搜索结果列表（带类型标签）
     */
    public List<Map<String, Object>> globalSearch(String keyword, int limit) {
        String kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> results = new ArrayList<>();

        addResults(results, searchPerson(kw, limit), "person", 40);
        addResults(results, searchEvent(kw, limit), "event", 30);
        addResults(results, searchLiterature(kw, limit), "literature", 50);
        addResults(results, searchAllusion(kw, limit), "allusion", 20);
        addResults(results, searchArticle(kw, limit), "article", 60);

        results.sort((a, b) -> {
            Integer sa = (Integer) a.get("score");
            Integer sb = (Integer) b.get("score");
            return sb.compareTo(sa);
        });

        return results.size() > limit ? results.subList(0, limit) : results;
    }

    /**
     * 推荐内容：基于用户收藏和浏览记录推荐相似资源
     * @param userId 用户ID（可选，为null时返回热门内容）
     * @param limit 返回条数
     * @return 推荐列表
     */
    public List<Map<String, Object>> recommend(Integer userId, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (userId != null) {
            // 存储用户喜欢的内容类型和关键词
            Set<String> likedTypes = new HashSet<>();
            Set<String> likedKeywords = new HashSet<>();

            // 查询用户的收藏记录
            List<com.sanguo.entity.Collection> cols = collectionMapper.selectList(
                    new LambdaQueryWrapper<com.sanguo.entity.Collection>().eq(com.sanguo.entity.Collection::getUserId, userId));
            // 分析用户收藏的内容类型
            for (com.sanguo.entity.Collection c : cols) {
                if (c.getTargetType() == Constants.COLLECT_PERSON) likedTypes.add("person");
                else if (c.getTargetType() == Constants.COLLECT_EVENT) likedTypes.add("event");
                else if (c.getTargetType() == Constants.COLLECT_LITERATURE) likedTypes.add("literature");
                else if (c.getTargetType() == Constants.COLLECT_ALLUSION) likedTypes.add("allusion");
            }
            // 如果用户没有收藏记录，返回热门内容
            if (likedTypes.isEmpty()) {
                return getHotContent(limit);
            }

            // 根据用户喜欢的类型获取推荐内容
            for (String type : likedTypes) {
                List<Map<String, Object>> items = getRecommendByType(type, limit / likedTypes.size() + 1);
                // 避免重复添加相同的内容
                for (Map<String, Object> item : items) {
                    if (!containsItem(results, item)) {
                        results.add(item);
                    }
                }
            }
        }
        // 如果推荐内容不足，用热门内容补充
        if (results.size() < limit) {
            List<Map<String, Object>> hot = getHotContent(limit - results.size());
            for (Map<String, Object> item : hot) {
                if (!containsItem(results, item)) {
                    results.add(item);
                }
            }
        }

        return results.size() > limit ? results.subList(0, limit) : results;
    }

    private List<Map<String, Object>> searchPerson(String kw, int limit) {
        LambdaQueryWrapper<Person> q = new LambdaQueryWrapper<>();
        q.and(w -> w.like(Person::getName, kw)
                .or().like(Person::getAlias, kw)
                .or().like(Person::getIntroduction, kw));
        q.orderByAsc(Person::getSort).orderByDesc(Person::getId);
        List<Person> list = personMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), q).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Person p : list) result.add(buildPersonMap(p));
        return result;
    }

    private List<Map<String, Object>> searchEvent(String kw, int limit) {
        LambdaQueryWrapper<Event> q = new LambdaQueryWrapper<>();
        q.and(w -> w.like(Event::getTitle, kw).or().like(Event::getContent, kw));
        q.orderByAsc(Event::getSort).orderByDesc(Event::getId);
        List<Event> list = eventMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), q).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Event e : list) result.add(buildEventMap(e));
        return result;
    }

    private List<Map<String, Object>> searchLiterature(String kw, int limit) {
        LambdaQueryWrapper<Literature> q = new LambdaQueryWrapper<>();
        q.and(w -> w.like(Literature::getTitle, kw).or().like(Literature::getAuthor, kw));
        q.orderByAsc(Literature::getSort).orderByDesc(Literature::getId);
        List<Literature> list = literatureMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), q).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Literature l : list) result.add(buildLiteratureMap(l));
        return result;
    }

    private List<Map<String, Object>> searchAllusion(String kw, int limit) {
        LambdaQueryWrapper<Allusion> q = new LambdaQueryWrapper<>();
        q.and(w -> w.like(Allusion::getTitle, kw).or().like(Allusion::getSource, kw));
        q.orderByAsc(Allusion::getSort).orderByDesc(Allusion::getId);
        List<Allusion> list = allusionMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), q).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Allusion a : list) result.add(buildAllusionMap(a));
        return result;
    }

    private List<Map<String, Object>> searchArticle(String kw, int limit) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        q.eq(Article::getStatus, Constants.ARTICLE_APPROVED);
        q.and(w -> w.like(Article::getTitle, kw).or().like(Article::getContent, kw));
        q.orderByDesc(Article::getViewCount).orderByDesc(Article::getCreateTime);
        List<Article> list = articleMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), q).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Article a : list) result.add(buildArticleMap(a));
        return result;
    }

    private List<Map<String, Object>> getRecommendByType(String type, int limit) {
        switch (type) {
            case "person":
                return searchPerson("", limit);
            case "event":
                return searchEvent("", limit);
            case "literature":
                return searchLiterature("", limit);
            case "allusion":
                return searchAllusion("", limit);
            default:
                return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getHotContent(int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<Person> persons = personMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit / 5 + 1),
                new LambdaQueryWrapper<Person>().orderByAsc(Person::getSort)).getRecords();
        for (Person p : persons) results.add(buildPersonMap(p));
        List<Event> events = eventMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit / 5 + 1),
                new LambdaQueryWrapper<Event>().orderByAsc(Event::getSort)).getRecords();
        for (Event e : events) results.add(buildEventMap(e));
        List<Literature> lits = literatureMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit / 5 + 1),
                new LambdaQueryWrapper<Literature>().orderByAsc(Literature::getSort)).getRecords();
        for (Literature l : lits) results.add(buildLiteratureMap(l));
        List<Allusion> als = allusionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit / 5 + 1),
                new LambdaQueryWrapper<Allusion>().orderByAsc(Allusion::getSort)).getRecords();
        for (Allusion a : als) results.add(buildAllusionMap(a));
        return results;
    }

    private void addResults(List<Map<String, Object>> results, List<Map<String, Object>> items, String type, int baseScore) {
        for (Map<String, Object> item : items) {
            item.put("type", type);
            item.put("score", baseScore + (item.get("score") != null ? (Integer) item.get("score") : 0));
            results.add(item);
        }
    }

    private boolean containsItem(List<Map<String, Object>> results, Map<String, Object> item) {
        Integer id = (Integer) item.get("id");
        String type = (String) item.get("type");
        for (Map<String, Object> r : results) {
            if (id.equals(r.get("id")) && type.equals(r.get("type"))) return true;
        }
        return false;
    }

    private Map<String, Object> buildPersonMap(Person p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("alias", p.getAlias());
        m.put("dynasty", p.getDynasty());
        m.put("achievements", p.getIntroduction());
        m.put("imageUrl", p.getImageUrl());
        m.put("type", "person");
        return m;
    }

    private Map<String, Object> buildEventMap(Event e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("title", e.getTitle());
        m.put("type_", e.getType());
        m.put("dynasty", e.getDynasty());
        m.put("content", e.getContent());
        m.put("imageUrl", e.getImageUrl());
        m.put("type", "event");
        return m;
    }

    private Map<String, Object> buildLiteratureMap(Literature l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("title", l.getTitle());
        m.put("author", l.getAuthor());
        m.put("category", l.getCategory());
        m.put("imageUrl", l.getImageUrl());
        m.put("type", "literature");
        return m;
    }

    private Map<String, Object> buildAllusionMap(Allusion a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("source", a.getSource());
        m.put("content", a.getContent());
        m.put("imageUrl", a.getImageUrl());
        m.put("type", "allusion");
        return m;
    }

    private Map<String, Object> buildArticleMap(Article a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("category", a.getCategory());
        m.put("coverUrl", a.getCoverUrl());
        m.put("viewCount", a.getViewCount());
        m.put("createTime", a.getCreateTime());
        m.put("type", "article");
        return m;
    }
}
