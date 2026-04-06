package com.guiderag.knowledge.service.impl;

import com.guiderag.knowledge.model.entity.DocumentChunk;
import com.guiderag.knowledge.config.EmbeddingConfig;
import com.guiderag.knowledge.service.ChunkingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.model.Tokenizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块服务实现类
 *
 * 算法原理：
 * LangChain4j 的 recursive splitter 会：
 * 1. 首先尝试按段落分割
 * 2. 如果段落过大，再按句子分割
 * 3. 如果句子仍过大，最终按字符分割
 * 4. 保证每块不超过 maxSegmentSizeInTokens
 * 5. 在块之间保留 maxOverlapSizeInTokens 的重叠
 * <p>
 * 为什么重叠？
 * 1. 避免关键信息在边界被切断
 * 2. 提高检索召回率（同一信息出现在多个块中）
 * 3. 保持语义连贯性
 *
 * @author Guide-RAG Team
 */
@Slf4j
@Service
public class ChunkingServiceImpl implements ChunkingService {

    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 50;

    private final EmbeddingConfig embeddingConfig;
    private final ObjectMapper objectMapper;

    private Tokenizer tokenizer;
    private DocumentSplitter documentSplitter;

    public ChunkingServiceImpl(EmbeddingConfig embeddingConfig) {
        this.embeddingConfig = embeddingConfig;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        String embeddingProvider = embeddingConfig.getProvider();
        String modelName = embeddingConfig.resolveCurrentModelName();
        log.info("[ChunkingService] 初始化 Tokenizer 和 DocumentSplitter: provider={}, model={}",
                embeddingProvider, modelName);

        switch (embeddingProvider.toLowerCase()) {
            case "openai":
                this.tokenizer = new OpenAiTokenizer(modelName);
                break;
            case "ollama", "gemini":
                this.tokenizer = new OpenAiTokenizer("gpt-3.5-turbo");
                log.warn("[ChunkingService] provider={} 使用 OpenAI Tokenizer 近似计算", embeddingProvider);
                break;
            default:
                this.tokenizer = new OpenAiTokenizer("gpt-3.5-turbo");
                log.warn("[ChunkingService] 未知 provider: {}, 使用默认 OpenAI Tokenizer", embeddingProvider);
        }

        this.documentSplitter = DocumentSplitters.recursive(
                CHUNK_SIZE, CHUNK_OVERLAP, tokenizer
        );

        log.info("[ChunkingService] 初始化完成: tokenizer={}, splitter={}",
                tokenizer.getClass().getSimpleName(),
                documentSplitter.getClass().getSimpleName());
    }

    @Override
    public List<DocumentChunk> chunkDocument(Long tenantId, Long documentId, String content) {
        log.info("[ChunkingService] 开始分块: documentId={}, contentLength={}",
                documentId, content == null ? 0 : content.length());

        if (content == null || content.isEmpty()) {
            log.warn("[ChunkingService] 文档内容为空: documentId={}", documentId);
            return new ArrayList<>();
        }

        Document document = Document.from(content);
        List<TextSegment> segments = documentSplitter.split(document);

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            int tokenCount = tokenizer.estimateTokenCountInText(segment.text());
            String vectorId = String.format("%d_%d_%d", tenantId, documentId, i);

            DocumentChunk chunk = DocumentChunk.builder()
                    .tenantId(tenantId)
                    .documentId(documentId)
                    .chunkIndex(i)
                    .modality("text")
                    .contentText(segment.text())
                    .tokenCount(tokenCount)
                    .embeddingModel(getEmbeddingModelName()) // 记录使用的模型
                    .vectorId(vectorId)
                    .metadata(buildMetadata(segment))
                    .gmtCreate(LocalDateTime.now())
                    .embeddingStatus(0)
                    .isDeleted(0)
                    .build();

            chunks.add(chunk);
        }

        // 统计信息
        int totalTokens = chunks.stream().mapToInt(DocumentChunk::getTokenCount).sum();
        double avgTokens = chunks.isEmpty() ? 0 : (double) totalTokens / chunks.size();

        log.info("[ChunkingService] 分块完成: documentId={}, totalChunks={}, totalTokens={}, avgTokens={}",
                documentId, chunks.size(), totalTokens, String.format("%.1f", avgTokens));

        return chunks;
    }

    // 获取Embedding模型
    private String getEmbeddingModelName() {
        String embeddingProvider = embeddingConfig.getProvider();
        String modelName = embeddingConfig.resolveCurrentModelName();
        if ("openai".equalsIgnoreCase(embeddingProvider)) {
            return modelName;
        } else if ("ollama".equalsIgnoreCase(embeddingProvider)) {
            return modelName;
        } else if ("gemini".equalsIgnoreCase(embeddingProvider)) {
            return modelName;
        } else {
            return modelName;
        }
    }

    // 构建元数据：将 LangChain4j Metadata 对象序列化为合法 JSON 字符串
    private String buildMetadata(TextSegment segment) {
        try {
            if (segment.metadata() != null && !segment.metadata().toMap().isEmpty()) {
                // 使用 Jackson 将 Map 序列化为标准 JSON，确保 PostgreSQL JSONB 可正常解析
                return objectMapper.writeValueAsString(segment.metadata().toMap());
            }
        } catch (JsonProcessingException e) {
            log.warn("[ChunkingService] metadata 序列化失败，将使用空对象: {}", e.getMessage());
        }
        return "{}";
    }
}
