# Milvus 向量数据库启动指南

## 问题说明
应用启动失败，错误信息：
```
DEADLINE_EXCEEDED: deadline exceeded after 9.968150600s
Failed to initialize connection to Milvus at 127.0.0.1:19530
```

**原因**：Milvus 向量数据库服务未运行，这是 rag-knowledge 服务的必需依赖。

---

## 解决方案

### 方案1：使用 Docker Compose 启动 Milvus（推荐）

#### 前置条件
- 已安装 Docker Desktop（Windows）或 Docker Engine（Linux）
- Docker 服务已启动

#### 启动步骤

1. **进入项目根目录**
```bash
cd D:\guide-rag
```

2. **启动 Milvus 及其依赖**
```bash
docker-compose -f docker-compose.milvus.yml up -d
```

3. **验证服务状态**
```bash
docker-compose -f docker-compose.milvus.yml ps
```

预期输出（所有服务状态为 healthy）：
```
NAME                IMAGE                           STATUS
milvus-standalone   milvusdb/milvus:v2.4.0         Up (healthy)
milvus-etcd         quay.io/coreos/etcd:v3.5.5     Up (healthy)
milvus-minio        minio/minio:...                Up (healthy)
```

4. **测试连接**
```bash
# Windows PowerShell
Test-NetConnection 127.0.0.1 -Port 19530

# 或使用 telnet（如果已安装）
telnet 127.0.0.1 19530
```

5. **重新启动应用**
现在可以重新运行 `KnowledgeApplication`，应该能正常连接到 Milvus。

---

### 方案2：如果没有 Docker

如果无法使用 Docker，可以：

#### 选项 A：安装 Milvus Standalone（本地二进制）
访问官方文档：https://milvus.io/docs/install_standalone-docker.md

#### 选项 B：暂时禁用 Milvus 依赖（不推荐）
修改应用配置，让 MilvusConfig Bean 变为可选：

在 `rag-knowledge/src/main/java/com/guiderag/knowledge/config/MilvusConfig.java` 中：
- 将 `@Bean` 改为 `@Bean @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")`
- 在 `application-dev.yml` 中添加 `milvus.enabled: false`

**注意**：这将导致文档向量化和检索功能完全不可用。

---

## 常用操作

### 停止 Milvus
```bash
docker-compose -f docker-compose.milvus.yml down
```

### 停止并删除数据（谨慎！）
```bash
docker-compose -f docker-compose.milvus.yml down -v
```

### 查看日志
```bash
# 查看 Milvus 日志
docker logs milvus-standalone

# 实时跟踪日志
docker logs -f milvus-standalone
```

### 重启 Milvus
```bash
docker restart milvus-standalone
```

---

## 端口说明

| 服务    | 端口  | 用途                     |
|---------|-------|--------------------------|
| Milvus  | 19530 | gRPC API（应用连接）     |
| Milvus  | 9091  | Health Check / Metrics   |
| MinIO   | 9000  | S3 兼容存储（内部使用）  |
| MinIO   | 9001  | MinIO 控制台（可选访问） |
| etcd    | 2379  | 元数据存储（内部使用）   |

---

## 故障排查

### 问题1：端口 19530 已被占用
```bash
# 查看占用进程
netstat -ano | findstr "19530"

# 修改配置使用其他端口（不推荐）
# 或停止占用该端口的其他服务
```

### 问题2：Docker 容器启动失败
```bash
# 查看详细错误
docker-compose -f docker-compose.milvus.yml logs

# 清理并重新创建
docker-compose -f docker-compose.milvus.yml down -v
docker-compose -f docker-compose.milvus.yml up -d
```

### 问题3：连接超时但容器正在运行
```bash
# 检查容器健康状态
docker inspect milvus-standalone | findstr "Health"

# 等待容器完全启动（首次启动需要 1-2 分钟）
docker-compose -f docker-compose.milvus.yml ps
```

---

## 生产环境建议

- 使用 Milvus 集群模式（分布式）
- 配置持久化存储卷
- 启用认证和 TLS
- 调整资源限制（CPU、内存）
- 定期备份数据

参考官方文档：https://milvus.io/docs/install_cluster-helm.md
