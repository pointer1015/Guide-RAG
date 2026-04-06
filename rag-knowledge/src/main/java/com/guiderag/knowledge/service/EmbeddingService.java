package com.guiderag.knowledge.service;

import java.util.List;

/**
 * 向量生成服务
 * 负责将文本转换为向量表示
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量
     * @param text 输入文本
     * @return 向量列表（维度取决于模型，BGE-M3 为 1024 维）
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量
     *
     * @param texts 输入文本列表
     * @return 向量列表的列表
     */
    List<float[]> embedBatch(List<String> texts);

}
