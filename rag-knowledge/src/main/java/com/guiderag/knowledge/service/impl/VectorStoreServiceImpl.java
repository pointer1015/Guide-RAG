package com.guiderag.knowledge.service.impl;

import com.guiderag.knowledge.service.VectorStoreService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.grpc.CollectionSchema;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.FieldSchema;
import io.milvus.grpc.KeyValuePair;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 向量存储服务实现
 * <p>
 * 核心功能:
 * 1. Collection 初始化与索引创建
 * 2. 向量数据插入 (upsert)
 * 3. 向量相似度搜索 (search)
 * 4. 向量数据删除 (delete)
 *
 * @author Guide-RAG Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    // Milvus 客户端，用于与 Milvus 向量数据库通信
    private final MilvusServiceClient milvusClient;

    // Collection 名称
    @Value("${milvus.collection-name:guide_rag_chunks}")
    private String collectionName;


    // 向量维度
    @Value("${milvus.dimension:1536}")
    private int dimension;

    // 相似度度量类型
    @Value("${milvus.metric-type:COSINE}")
    private String metricType;

    // 服务启动时初始化 Collection
    @PostConstruct
    public void init() {
        ensureCollectionExists();
    }

    /**
     * 确保 Collection 存在，不存在则创建。
     * 若已存在但维度与配置不符，则自动删除旧 Collection 并重建。
     */
    private void ensureCollectionExists() {
        try {
            // 检查 Collection 是否已存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (hasCollection.getData()) {
                // 校验已有 Collection 的 embedding 字段维度是否与配置一致
                int existingDim = getExistingEmbeddingDimension();
                if (existingDim > 0 && existingDim != dimension) {
                    log.warn("[Milvus] Collection '{}' 的 embedding 维度为 {}，与配置维度 {} 不符，自动删除并重建",
                            collectionName, existingDim, dimension);
                    // 先释放内存中的 Collection，再删除
                    milvusClient.releaseCollection(
                            ReleaseCollectionParam.newBuilder()
                                    .withCollectionName(collectionName)
                                    .build()
                    );
                    R<RpcStatus> dropResult = milvusClient.dropCollection(
                            DropCollectionParam.newBuilder()
                                    .withCollectionName(collectionName)
                                    .build()
                    );
                    handleResult(dropResult, "删除旧 Collection");
                    log.info("[Milvus] 旧 Collection '{}' 已删除，将重新创建", collectionName);
                } else {
                    log.info("[Milvus] Collection '{}' 已存在（维度: {}），直接加载", collectionName, existingDim);
                    loadCollection();
                    return;
                }
            }

            log.info("[Milvus] 创建 Collection: {}, 维度: {}", collectionName, dimension);

            // 定义字段
            List<FieldType> fieldTypes = Arrays.asList(
                    // 主键 vector_id
                    FieldType.newBuilder()
                            .withName("vector_id")
                            .withDataType(DataType.VarChar)
                            .withMaxLength(128)
                            .withPrimaryKey(true)
                            .withAutoID(false)
                            .build(),
                    // 租户Id
                    FieldType.newBuilder()
                            .withName("tenant_id")
                            .withDataType(DataType.Int64)
                            .build(),
                    // 文档Id
                    FieldType.newBuilder()
                            .withName("document_id")
                            .withDataType(DataType.Int64)
                            .build(),
                    // 分块索引
                    FieldType.newBuilder()
                            .withName("chunk_index")
                            .withDataType(DataType.Int32)
                            .build(),
                    // 文本内容
                    FieldType.newBuilder()
                            .withName("content_text")
                            .withDataType(DataType.VarChar)
                            .withMaxLength(65535)
                            .build(),
                    // 向量字段
                    FieldType.newBuilder()
                            .withName("embedding")
                            .withDataType(DataType.FloatVector)
                            .withDimension(dimension)
                            .build()
            );

            // 创建Collection Schema
            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .withFieldTypes(fieldTypes)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSchema(schema)
                    .build();

            // 【修复】实际执行创建 Collection
            R<RpcStatus> createResult = milvusClient.createCollection(createParam);
            handleResult(createResult, "创建 Collection");
            log.info("[Milvus] Collection '{}' 创建成功", collectionName);

            // 创建索引
            createIndex();

            // 加载 Collection 到内存
            loadCollection();

            log.info("[Milvus] Collection '{}' 创建并加载完成", collectionName);
        } catch (Exception e) {
            log.error("[Milvus] Collection 初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Milvus 初始化失败", e);
        }
    }

    /**
     * 查询已有 Collection 中 embedding 字段的实际维度。
     * 通过解析 FieldSchema 的 type_params（"dim" 键）获取。
     *
     * @return 实际维度，若获取失败则返回 -1
     */
    private int getExistingEmbeddingDimension() {
        try {
            R<DescribeCollectionResponse> resp = milvusClient.describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (resp.getStatus() != R.Status.Success.getCode() || resp.getData() == null) {
                log.warn("[Milvus] 无法获取 Collection 描述信息，跳过维度校验");
                return -1;
            }
            CollectionSchema schema = resp.getData().getSchema();
            for (FieldSchema field : schema.getFieldsList()) {
                if ("embedding".equals(field.getName())) {
                    for (KeyValuePair kv : field.getTypeParamsList()) {
                        if ("dim".equals(kv.getKey())) {
                            return Integer.parseInt(kv.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Milvus] 获取 embedding 维度时出错: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * 创建向量索引
     * <p>
     * 使用 IVF_FLAT 索引:
     * - nlist: 聚类中心数量（推荐 4*sqrt(n)）
     * - 适合中等规模数据集（百万级）
     * <p>
     * 对于更大规模可切换为 HNSW:
     * - M: 每个节点的最大连接数
     * - efConstruction: 构建时的动态列表大小
     */
    private void createIndex() {
        log.info("[Milvus] 创建向量索引, 类型: IVF_FLAT, 度量: {}", metricType);
        
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.valueOf(metricType))
                .withExtraParam("{\"nlist\": 1024}")
                .build();
        
        // 【修复】实际执行创建索引
        R<RpcStatus> result = milvusClient.createIndex(indexParam);
        handleResult(result, "创建向量索引");
        log.info("[Milvus] 向量索引创建成功");
    }

    /**
     * 加载 Collection 到内存
     */
    private void loadCollection() {
        log.info("[Milvus] 加载 Collection '{}' 到内存", collectionName);

        R<RpcStatus> result = milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        handleResult(result, "加载 Collection");
    }


    /**
     * 插入/更新向量数据
     *
     * @param vectorId    向量唯一标识: {tenantId}_{documentId}_{chunkIndex}
     * @param tenantId    租户ID
     * @param documentId  文档ID
     * @param chunkIndex  分块索引
     * @param contentText 原始文本内容
     * @param embedding   向量数据
     */
    @Override
    public void upsert(String vectorId, Long tenantId, Long documentId, int chunkIndex, String contentText, float[] embedding) {
        log.debug("[Milvus] Upsert 向量: vectorId={}, tenantId={}, docId={}",
                vectorId, tenantId, documentId);

        // 尝试删除已存在的记录
        delete(vectorId);

        // 准备插入数据
        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("vector_id", Collections.singletonList(vectorId)),
                new InsertParam.Field("tenant_id", Collections.singletonList(tenantId)),
                new InsertParam.Field("document_id", Collections.singletonList(documentId)),
                new InsertParam.Field("chunk_index", Collections.singletonList(chunkIndex)),
                new InsertParam.Field("content_text", Collections.singletonList(contentText)),
                new InsertParam.Field("embedding", Collections.singletonList(toFloatList(embedding)))
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> result = milvusClient.insert(insertParam);
        handleResult(result, "插入向量");
    }

    /**
     * 批量插入向量数据
     *
     * @param entries 向量数据列表
     */
    @Override
    public void upsertBatch(List<VectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        log.info("[Milvus] 批量插入 {} 条向量", entries.size());

        // 批量删除已存在的记录
        List<String> vectorIds = entries.stream()
                .map(VectorEntry::getVectorId)
                .toList();
        deleteBatch(vectorIds);

        // 准备批量数据
        List<String> vectorIdList = new ArrayList<>();
        List<Long> tenantIdList = new ArrayList<>();
        List<Long> documentIdList = new ArrayList<>();
        List<Integer> chunkIndexList = new ArrayList<>();
        List<String> contentTextList = new ArrayList<>();
        List<List<Float>> embeddingList = new ArrayList<>();

        for (VectorEntry entry : entries) {
            vectorIdList.add(entry.getVectorId());
            tenantIdList.add(entry.getTenantId());
            documentIdList.add(entry.getDocumentId());
            chunkIndexList.add(entry.getChunkIndex());
            contentTextList.add(entry.getContentText());
            embeddingList.add(toFloatList(entry.getEmbedding()));
        }

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("vector_id", vectorIdList),
                new InsertParam.Field("tenant_id", tenantIdList),
                new InsertParam.Field("document_id", documentIdList),
                new InsertParam.Field("chunk_index", chunkIndexList),
                new InsertParam.Field("content_text", contentTextList),
                new InsertParam.Field("embedding", embeddingList)
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> result = milvusClient.insert(insertParam);
        handleResult(result, "批量插入向量");

    }

    /**
     * 向量相似度搜索
     *
     * @param tenantId        租户ID（数据隔离）
     * @param knowledgeBaseId 知识库ID（可选过滤）
     * @param queryVector     查询向量
     * @param topK            返回数量
     * @param minScore        最低相似度阈值 (0-1)
     * @return 搜索结果列表
     */
    @Override
    public List<SearchResult> search(Long tenantId, Long knowledgeBaseId, float[] queryVector, int topK, float minScore) {

        log.info("[Milvus] 向量搜索: tenantId={}, topK={}, minScore={}",
                tenantId, topK, minScore);

        // 构建过滤表达式（多租户隔离）
        String filterExpr = String.format("tenant_id == %d", tenantId);

        // 搜索参数
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.valueOf(metricType))
                .withOutFields(Arrays.asList("vector_id", "document_id", "chunk_index", "content_text"))
                .withTopK(topK)
                .withVectors(Collections.singletonList(toFloatList(queryVector)))
                .withVectorFieldName("embedding")
                .withExpr(filterExpr)
                .withParams("{\"nprobe\": 16}") // IVF_FLAT 搜索参数
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        handleResult(response, "向量搜索");

        // 解析结果
        List<SearchResult> searchResults = new ArrayList<>();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            QueryResultsWrapper.RowRecord record = wrapper.getRowRecords(0).get(i);
            float score = wrapper.getIDScore(0).get(i).getScore();

            // 对于 COSINE 相似度，score 已经是 [0,1] 范围
            // 对于 L2 距离，需要转换: similarity = 1 / (1 + distance)
            float similarity = metricType.equals("L2") ? 1.0f / (1.0f + score) : score;

            if (similarity >= minScore) {
                SearchResult sr = new SearchResult((String) record.get("vector_id"),
                        (Long) record.get("document_id"),
                        (Integer) record.get("chunk_index"),
                        (String) record.get("content_text"),
                        similarity);

                searchResults.add(sr);
            }
        }

        log.info("[Milvus] 搜索完成，返回 {} 条结果（过滤后）", searchResults.size());
        return searchResults;
    }

    /**
     * 删除单条向量
     */
    @Override
    public void delete(String vectorId) {
        String expr = String.format("vector_id == \"%s\"", vectorId);

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        milvusClient.delete(deleteParam);
    }

    /**
     * 批量删除向量
     */
    @Override
    public void deleteBatch(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }

        String idsStr = vectorIds.stream()
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String expr = String.format("vector_id in [%s]", idsStr);

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        milvusClient.delete(deleteParam);
    }

    /**
     * 删除文档的所有向量
     */
    @Override
    public void deleteByDocumentId(Long tenantId, Long documentId) {
        String expr = String.format("tenant_id == %d && document_id == %d", tenantId, documentId);

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        R<MutationResult> result = milvusClient.delete(deleteParam);
        handleResult(result, "删除文档向量");

        log.info("[Milvus] 已删除文档 {} 的所有向量", documentId);
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private <T> void handleResult(R<T> result, String operation) {
        if (result.getStatus() != R.Status.Success.getCode()) {
            String errorMsg = String.format("[Milvus] %s 失败: %s", operation, result.getMessage());
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }


}
