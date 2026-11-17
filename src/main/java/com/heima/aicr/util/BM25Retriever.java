package com.heima.aicr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * BM25 检索工具类
 * 实现 BM25 算法进行关键词检索
 * 
 * BM25 是经典的信息检索排序算法，相比 TF-IDF 考虑了文档长度归一化
 * 公式：score = IDF * (f * (k1 + 1)) / (f + k1 * (1 - b + b * dl/avgdl))
 * 
 * 其中：
 * - f: 词项在文档中的频率
 * - dl: 文档长度
 * - avgdl: 平均文档长度
 * - k1: 控制词频饱和度的参数（通常 1.2-2.0）
 * - b: 控制文档长度归一化的参数（通常 0.75）
 */
@Slf4j
@Component
public class BM25Retriever {

    // BM25 参数
    private static final double K1 = 1.5;  // 词频饱和度参数
    private static final double B = 0.75;  // 长度归一化参数

    /**
     * 文档结构
     */
    public static class Document {
        private final String id;
        private final String content;
        private final Map<String, Integer> termFreq;  // 词频
        private final int length;  // 文档长度（词数）

        public Document(String id, String content) {
            this.id = id;
            this.content = content;
            this.termFreq = calculateTermFreq(content);
            this.length = this.termFreq.values().stream().mapToInt(Integer::intValue).sum();
        }

        private Map<String, Integer> calculateTermFreq(String content) {
            Map<String, Integer> freq = new HashMap<>();
            String[] terms = tokenize(content);
            for (String term : terms) {
                freq.merge(term, 1, Integer::sum);
            }
            return freq;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public Map<String, Integer> getTermFreq() { return termFreq; }
        public int getLength() { return length; }
    }

    /**
     * 检索结果
     */
    public static class SearchResult {
        private final String docId;
        private final String content;
        private final double score;

        public SearchResult(String docId, String content, double score) {
            this.docId = docId;
            this.content = content;
            this.score = score;
        }

        public String getDocId() { return docId; }
        public String getContent() { return content; }
        public double getScore() { return score; }
    }

    private List<Document> documents = new ArrayList<>();
    private double avgDocLength = 0;
    private Map<String, Integer> docFreq = new HashMap<>();  // 文档频率

    /**
     * 添加文档到索引
     */
    public void addDocument(String id, String content) {
        Document doc = new Document(id, content);
        documents.add(doc);
        
        // 更新文档频率
        for (String term : doc.getTermFreq().keySet()) {
            docFreq.merge(term, 1, Integer::sum);
        }
        
        // 重新计算平均文档长度
        updateAvgDocLength();
    }

    /**
     * 批量添加文档
     */
    public void addDocuments(Map<String, String> docMap) {
        for (Map.Entry<String, String> entry : docMap.entrySet()) {
            addDocument(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 检索（返回topK个结果）
     */
    public List<SearchResult> search(String query, int topK) {
        if (documents.isEmpty() || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] queryTerms = tokenize(query);
        
        // 计算每个文档的 BM25 分数
        List<SearchResult> results = new ArrayList<>();
        for (Document doc : documents) {
            double score = calculateBM25Score(doc, queryTerms);
            if (score > 0) {
                results.add(new SearchResult(doc.getId(), doc.getContent(), score));
            }
        }

        // 按分数降序排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // 返回 topK
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 计算 BM25 分数
     */
    private double calculateBM25Score(Document doc, String[] queryTerms) {
        double score = 0.0;
        
        for (String term : queryTerms) {
            // 计算 IDF
            double idf = calculateIDF(term);
            
            // 获取词频
            int f = doc.getTermFreq().getOrDefault(term, 0);
            
            // 计算 BM25 分数
            double numerator = f * (K1 + 1);
            double denominator = f + K1 * (1 - B + B * doc.getLength() / avgDocLength);
            
            score += idf * numerator / denominator;
        }
        
        return score;
    }

    /**
     * 计算 IDF（逆文档频率）
     * 使用平滑公式：log((N - n + 0.5) / (n + 0.5) + 1)
     */
    private double calculateIDF(String term) {
        int n = docFreq.getOrDefault(term, 0);  // 包含该词的文档数
        int N = documents.size();  // 总文档数
        
        if (n == 0) {
            return 0;
        }
        
        return Math.log((N - n + 0.5) / (n + 0.5) + 1);
    }

    /**
     * 更新平均文档长度
     */
    private void updateAvgDocLength() {
        if (documents.isEmpty()) {
            avgDocLength = 0;
            return;
        }
        
        int totalLength = documents.stream()
                .mapToInt(Document::getLength)
                .sum();
        avgDocLength = (double) totalLength / documents.size();
    }

    /**
     * 分词（简单实现，实际项目中可使用 IK 分词器等）
     */
    private static String[] tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }
        
        // 转换为小写，移除非字母数字字符
        text = text.toLowerCase();
        
        // 中文分词：按字符分割（简单处理）
        // 英文分词：按空格和标点分割
        List<String> tokens = new ArrayList<>();
        
        // 提取中文字符
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                tokens.add(String.valueOf(c));
            }
        }
        
        // 提取英文单词
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (word.length() >= 2) {  // 过滤单字符
                tokens.add(word);
            }
        }
        
        return tokens.toArray(new String[0]);
    }

    /**
     * 清空索引
     */
    public void clear() {
        documents.clear();
        docFreq.clear();
        avgDocLength = 0;
    }

    /**
     * 获取索引文档数量
     */
    public int getDocumentCount() {
        return documents.size();
    }
}
