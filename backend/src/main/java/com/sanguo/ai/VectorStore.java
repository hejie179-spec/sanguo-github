package com.sanguo.ai;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Component
@Slf4j
public class VectorStore {

    /**
     * 本地“向量知识库”（用于 AI 检索增强）
     *
     * <p>做什么：</p>
     * <ul>
     *   <li>读取 dataset 文本（优先 uploads/dataset，其次 resources/dataset/*.txt）</li>
     *   <li>将文本按 CHUNK_SIZE 切块，调用 embedding 得到向量</li>
     *   <li>把向量与原文缓存到 uploads/vector_cache.json，避免每次启动都重新 embedding</li>
     *   <li>search() 时按相似度返回 topK 段文本，作为大模型回答的“参考资料”</li>
     * </ul>
     *
     * <p>为什么启动时可能搜不到：</p>
     * <ul>
     *   <li>init() 里用新线程异步初始化，初始化完成前 initialized=false</li>
     *   <li>首次构建需要调用 embedding，速度取决于网络与模型服务</li>
     * </ul>
     */
    private List<Document> documents = new ArrayList<>();
    private static final int CHUNK_SIZE = 500;
    private static final int VECTOR_LIMIT = 400;
    private static final int BATCH_SIZE = 20;
    private volatile boolean initialized = false;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Resource
    private ZhipuClient zhipuClient;

    @PostConstruct
    public void init() {
        new Thread(this::loadOrBuild).start();
    }

    private void loadOrBuild() {
        try {
            List<DatasetText> datasetTexts = loadDatasetTexts();
            String fingerprint = computeFingerprint(datasetTexts);
            File cache = cacheFile();
            if (cache.exists()) {
                log.info("检测到本地向量缓存，尝试加载: {} (user.dir={})", cache.getAbsolutePath(), System.getProperty("user.dir"));
                try {
                    String json = FileUtil.readUtf8String(cache);
                    VectorCache vectorCache = tryParseVectorCache(json);
                    if (vectorCache != null && fingerprint.equals(vectorCache.getFingerprint()) && vectorCache.getDocuments() != null) {
                        documents = vectorCache.getDocuments();
                        initialized = true;
                        log.info("向量缓存加载完成，共 {} 条数据", documents.size());
                        return;
                    }
                    log.info("向量缓存与当前数据不匹配，开始重建向量缓存");
                    buildFromDataset(datasetTexts, fingerprint);
                } catch (Exception e) {
                    log.error("加载本地向量缓存失败，将重新构建", e);
                    buildFromDataset(datasetTexts, fingerprint);
                }
            } else {
                log.info("未发现本地向量缓存，开始构建向量缓存，目标文件: {} (user.dir={})", cache.getAbsolutePath(), System.getProperty("user.dir"));
                buildFromDataset(datasetTexts, fingerprint);
            }
        } catch (Exception e) {
            log.error("初始化向量库失败", e);
        }
    }

    private void buildFromDataset(List<DatasetText> datasetTexts, String fingerprint) {
        try {
            if (datasetTexts == null || datasetTexts.isEmpty()) {
                documents = new ArrayList<>();
                initialized = true;
                log.warn("未加载到任何dataset文本，向量库将为空");
                return;
            }
            List<String> allChunks = new ArrayList<>();
            for (DatasetText dt : datasetTexts) {
                if (dt == null || dt.text == null) continue;
                allChunks.addAll(chunkText(dt.text, CHUNK_SIZE));
                log.info("读取文件 {} 完成", dt.name);
            }

            log.info("总计切分文本块: {}", allChunks.size());

            int limit = Math.min(allChunks.size(), VECTOR_LIMIT);
            List<String> chunksToProcess = allChunks.subList(0, limit);

            List<Document> newDocs = new ArrayList<>(chunksToProcess.size());
            for (int i = 0; i < chunksToProcess.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, chunksToProcess.size());
                List<String> batch = chunksToProcess.subList(i, end);
                try {
                    List<List<Double>> embeddings = zhipuClient.embed(batch);
                    for (int j = 0; j < batch.size(); j++) {
                        List<Double> vec = (embeddings != null && j < embeddings.size()) ? embeddings.get(j) : null;
                        newDocs.add(new Document(batch.get(j), vec));
                    }
                    Thread.sleep(1000);
                    log.info("向量化进度: {}/{}", end, chunksToProcess.size());
                } catch (Exception e) {
                    log.error("向量化批处理失败, batch: {}-{}", i, end, e);
                    for (String content : batch) {
                        newDocs.add(new Document(content, null));
                    }
                }
            }

            File cache = cacheFile();
            FileUtil.touch(cache);
            FileUtil.writeUtf8String(JSONUtil.toJsonStr(new VectorCache(fingerprint, newDocs)), cache);
            documents = newDocs;
            initialized = true;
            log.info("知识库向量化完成，已保存至 {}", cache.getAbsolutePath());

        } catch (Exception e) {
            log.error("构建向量库异常", e);
        }
    }

    private VectorCache tryParseVectorCache(String json) {
        if (json == null) return null;
        String s = json.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("[")) return null;
        if (!s.startsWith("{")) return null;
        return JSONUtil.toBean(s, VectorCache.class);
    }

    private static class DatasetText {
        private final String name;
        private final String text;

        private DatasetText(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    private List<DatasetText> loadDatasetTexts() {
        List<DatasetText> list = new ArrayList<>();

        File externalDir = externalDatasetDir();
        if (externalDir.exists() && externalDir.isDirectory()) {
            List<File> files = FileUtil.loopFiles(externalDir, file -> file.isFile() && file.getName().toLowerCase().endsWith(".txt"));
            for (File f : files) {
                try {
                    String text = FileUtil.readUtf8String(f);
                    if (text != null && !text.trim().isEmpty()) {
                        list.add(new DatasetText(f.getName(), text));
                    }
                } catch (Exception e) {
                    log.warn("读取外部dataset文件失败: {}", f.getAbsolutePath(), e);
                }
            }
            if (!list.isEmpty()) {
                log.info("从外部目录加载dataset文件: {}", externalDir.getAbsolutePath());
                return list;
            }
        }

        try {
            URL url = this.getClass().getClassLoader().getResource("dataset");
            if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
                File dir = new File(url.toURI());
                if (dir.exists() && dir.isDirectory()) {
                    File[] fs = dir.listFiles((d, name) -> name != null && name.toLowerCase().endsWith(".txt"));
                    if (fs != null) {
                        for (File f : fs) {
                            try {
                                String text = FileUtil.readUtf8String(f);
                                if (text != null && !text.trim().isEmpty()) {
                                    list.add(new DatasetText("dataset/" + f.getName(), text));
                                }
                            } catch (Exception e) {
                                log.warn("读取classpath dataset文件失败: {}", f.getAbsolutePath(), e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("扫描classpath dataset目录失败", e);
        }

        if (!list.isEmpty()) return list;

        String[] fallbackFiles = {"dataset/三国志.txt", "dataset/三国演义.txt"};
        for (String file : fallbackFiles) {
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(file)) {
                if (is == null) continue;
                String text = IoUtil.readUtf8(is);
                if (text != null && !text.trim().isEmpty()) {
                    list.add(new DatasetText(file, text));
                }
            } catch (Exception e) {
                log.warn("读取fallback dataset文件失败: {}", file, e);
            }
        }
        return list;
    }

    private File cacheFile() {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        return root.resolve("vector_cache.json").toFile();
    }

    private File externalDatasetDir() {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        return root.resolve("dataset").toFile();
    }

    private String computeFingerprint(List<DatasetText> datasetTexts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("chunk=" + CHUNK_SIZE + ";limit=" + VECTOR_LIMIT).getBytes(StandardCharsets.UTF_8));
            if (datasetTexts != null) {
                for (DatasetText dt : datasetTexts) {
                    if (dt == null) continue;
                    md.update((dt.name == null ? "" : dt.name).getBytes(StandardCharsets.UTF_8));
                    md.update((byte) 0);
                    md.update((dt.text == null ? "" : dt.text).getBytes(StandardCharsets.UTF_8));
                    md.update((byte) 0);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                String h = Integer.toHexString(b & 0xff);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private List<String> chunkText(String text, int size) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        StringBuilder current = new StringBuilder();
        for (String p : paragraphs) {
            p = p.trim();
            if (p.isEmpty()) continue;
            if (current.length() + p.length() > size) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
            }
            current.append(p).append(" ");
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }

    public List<String> search(String query, int topK) {
        if (!initialized || documents.isEmpty()) {
            log.warn("向量库尚未初始化完成或为空");
            return Collections.emptyList();
        }
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        boolean hasEmbedding = false;
        for (Document d : documents) {
            if (d.getEmbedding() != null && !d.getEmbedding().isEmpty()) {
                hasEmbedding = true;
                break;
            }
        }
        try {
            if (hasEmbedding) {
                List<Double> queryVec = zhipuClient.embed(Collections.singletonList(query)).get(0);
                PriorityQueue<Map.Entry<Double, String>> pq = new PriorityQueue<>(Map.Entry.comparingByKey());

                for (Document doc : documents) {
                    if (doc.getEmbedding() == null || doc.getEmbedding().isEmpty()) continue;
                    double sim = cosineSimilarity(queryVec, doc.getEmbedding());
                    pq.offer(new AbstractMap.SimpleEntry<>(sim, doc.getContent()));
                    if (pq.size() > topK) {
                        pq.poll();
                    }
                }

                List<String> res = new ArrayList<>();
                while (!pq.isEmpty()) {
                    res.add(pq.poll().getValue());
                }
                Collections.reverse(res);
                if (!res.isEmpty()) return res;
            }

            return keywordSearch(query, topK);
        } catch (Exception e) {
            log.error("检索失败", e);
            return keywordSearch(query, topK);
        }
    }

    private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < vecA.size(); i++) {
            dot += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> keywordSearch(String query, int topK) {
        PriorityQueue<Map.Entry<Integer, String>> pq = new PriorityQueue<>(Map.Entry.comparingByKey());
        for (Document doc : documents) {
            String content = doc.getContent();
            if (content == null || content.isEmpty()) continue;
            int score = keywordScore(query, content);
            if (score <= 0) continue;
            pq.offer(new AbstractMap.SimpleEntry<>(score, content));
            if (pq.size() > topK) pq.poll();
        }
        List<String> res = new ArrayList<>();
        while (!pq.isEmpty()) res.add(pq.poll().getValue());
        Collections.reverse(res);
        return res;
    }

    private int keywordScore(String query, String content) {
        String q = query.replaceAll("[\\s\\p{Punct}]+", "");
        if (q.isEmpty()) return 0;
        int score = 0;
        if (q.length() <= 2) {
            return content.contains(q) ? 10 : 0;
        }
        for (int i = 0; i < q.length() - 1; i++) {
            String bg = q.substring(i, i + 2);
            if (content.contains(bg)) score += 1;
        }
        if (content.contains(q)) score += 5;
        return score;
    }
}
