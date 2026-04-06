# GuideRAG: Industrial Multi-modal RAG Knowledge Base Assistant 🚀

[中文版](./README.zh_CN.md) | [English](./README.md)

GuideRAG 是一款基于 **Spring Boot 3**、**Vue 3** 和 **LangChain4j** 构建的产业级多模态 RAG（检索增强生成）知识库系统。它旨在为企业提供高性能、可扩展的知识管理与 AI 问答能力。

## ✨ 核心特性

- 🛠️ **多模型支持**: 集成 OpenAI, Gemini, 以及本地 Ollama (支持 bge-m3 等模型)。
- 🧠 **先进 RAG 架构**:
  - 向量数据库: **Milvus 2.4+**。
  - 灵活分词策略: 支持多种 Chunking 模式。
  - 语义检索与重排序。
- 🏗️ **微服务架构**: 基于 Spring Cloud Alibaba，模块清晰，支持横向扩展。
- 🎨 **现代 UI**: 使用 Vue 3 + TypeScript + Vite + Vanilla CSS，界面精美、响应迅速。

## 🛠️ 技术栈

### 后端 (Backend)
- **核心框架**: Java 21, Spring Boot 3.2.4, Spring Cloud 2023
- **AI 编排**: LangChain4j 0.36.2
- **数据存储**: PostgreSQL (关系型), Milvus (向量)
- **对象存储**: MinIO
- **中间件**: Redis, Nacos (可选)

### 前端 (Frontend)
- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **状态管理**: Pinia
- **开发语言**: TypeScript

## 🚀 快速开始

### 1. 环境准备
确保你已安装以下工具：
- JDK 21
- Maven 3.9+
- Docker & Docker Compose
- Node.js 18+

### 2. 启动中间件
```bash
docker-compose -f docker-compose.milvus.yml up -d
```
*注意：请确保 PostgreSQL 和 MinIO 已正确配置并启动。*

### 3. 后端启动
1. 复制配置文件模板：
   ```bash
   cp rag-knowledge/src/main/resources/application-dev.yml.template rag-knowledge/src/main/resources/application-dev.yml
   ```
2. 在 `application-dev.yml` 中填入你的 API Key 和数据库密码。
3. 编译并运行：
   ```bash
   mvn clean install
   # 分别启动各微服务模块
   ```

### 4. 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 📄 开源协议
本项目采用 [Apache-2.0](./LICENSE) 协议开源。

---
**GuideRAG - 让知识触手可及。**
