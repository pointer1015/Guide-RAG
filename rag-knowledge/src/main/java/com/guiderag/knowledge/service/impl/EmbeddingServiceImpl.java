package com.guiderag.knowledge.service.impl;

import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 LangChain4j 的 Embedding 服务实现
 * 支持 OpenAI、Ollama 等多种 Embedding 模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    // LangChain4j 嵌入模型
    private final EmbeddingModel embeddingModel;

    /**
     * 批量嵌入的最大批次大小
     * 避免单次请求过大导致超时或内存溢出
     */
    private static final int BATCH_SIZE = 50;

    /**
     * 单文本向量化
     *
     * @param text 待嵌入的文本内容
     * @return 向量数组（维度取决于模型，如 text-embedding-3-small 为 1536 维）
     */
    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("输入文本为空，无法生成向量");
            return new float[0];
        }

        try {
            log.debug("[Embedding] 开始嵌入文本，长度: {} 字符", text.length());

            // LangChain4j api：单挑文本嵌入
            Embedding content = embeddingModel.embed(text).content();
            float[] vector = content.vector();

            log.debug("[Embedding] 嵌入完成，向量维度: {}", vector.length);

            return vector;
        } catch (Exception e) {
            log.error("[Embedding] 向量生成失败: {}", e.getMessage(), e);
            throw new BizException("B0002", "向量生成失败，请稍后再试");
        }
    }

    /**
     * 批量文本向量化
     * <p>
     * 自动分批处理，避免单次请求过大
     * 适用于文档分块后的批量嵌入场景
     *
     * @param texts 文本列表
     * @return 向量列表，与输入文本一一对应
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("[Embedding] 空文本列表输入");
            return new ArrayList<>();
        }

        log.info("[Embedding] 开始批量嵌入，文本数量: {}", texts.size());

        List<float[]> allEmbeddings = new ArrayList<>();

        // 分批处理，每批最多batch_size条
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, endIndex);

            log.debug("[Embedding] 处理批次 {}/{}, 当前批次大小: {}",
                    (i / BATCH_SIZE) + 1,
                    (texts.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                    batch.size());

            try {
                // 转换为 LangChain4j 的 TextSegment 格式
                List<TextSegment> segments = batch.stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());

                // 批量嵌入 API
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();


                // 提取向量数组
                for (Embedding embedding : embeddings) {
                    allEmbeddings.add(embedding.vector());
                }

            } catch (Exception e) {
                log.error("[Embedding] 批量嵌入失败: {}", e.getMessage(), e);
                // 降级：逐条单独嵌入，全部失败则直接向上抛异常，禁止返回空向量（维度=0 会导致 Milvus 写入失败）
                for (String text : batch) {
                    try {
                        allEmbeddings.add(embed(text));
                    } catch (Exception ex) {
                        log.error("[Embedding] 单条降级失败，终止批量嵌入: {}", ex.getMessage());
                        throw new RuntimeException("Embedding 失败，无法生成向量: " + ex.getMessage(), ex);
                    }
                }
            }
        }
        log.info("[Embedding] 批量嵌入完成，成功生成 {} 个向量", allEmbeddings.size());
        return allEmbeddings;
    }

    /**
     * 获取嵌入模型的向量维度
     *
     * @return 向量维度（如 1536、768 等）
     */
    public int getDimension() {
        // 嵌入一个测试文本获取维度
        float[] testVector = embed("test");
        return testVector.length;

    }
}
