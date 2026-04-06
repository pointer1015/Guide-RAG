# GuideRAG 项目开源 GitHub 准备工作详尽指南

本指南旨在指导开发者如何安全、专业地将 **GuideRAG** 项目发布至 GitHub。请在上传前务必核对以下每一项内容。

---

## 一、 核心安全：敏感信息脱敏 (最高优先级) 🛡️

在代码公开之前，必须确保没有任何私钥、密码或 API 凭证被上传。

### 1. 配置文件脱敏
目前项目中 `rag-knowledge` 和 `rag-auth` 的 `application-dev.yml` 包含明文配置。
- **操作**: 
  1. 在每个模块的 `resources` 目录下创建 `application-dev.yml.template`。
  2. 将 `application-dev.yml` 中的敏感值替换为占位符（如 `${GEMINI_API_KEY:your_key_here}`）。
  3. **立即检查**: 确认 `rag-knowledge` 中的 `embedding.gemini.api-key` 已被移除。
- **Git 忽略**: 确保根目录的 `.gitignore` 包含以下规则：
  ```text
  **/src/main/resources/application-dev.yml
  **/src/main/resources/application-prod.yml
  ```

### 2. 清理 Git 历史记录
如果你曾 commit 过包含敏感信息的文件，仅仅修改它们是不够的，历史记录中依然可以被找回。
- **建议**: 如果是第一次推送到 GitHub 且不强求保留本地提交历史，建议在本地重新初始化 `git init`，然后整包推送到新仓库。

---

## 二、 文档体系建设 📄

GitHub 的 Readme 是项目的脸面。

### 1. 完善 README.md (必选)
请参考 `docs/README_TEMPLATE.md` 创建根目录的 `README.md`。
- **包含内容**: 架构图、技术栈、核心特性说明、快速启动步骤。
- **双语建议**: 建议维护 `README.md` (英文) 和 `README.zh_CN.md` (中文)。

### 2. 选择并添加 LICENSE (必选)
- **推荐**: **Apache-2.0** 协议（支持商用，对贡献者更友好）。
- **操作**: 在 GitHub 仓库创建页面直接选择，或在根目录手动创建 `LICENSE` 文件。

### 3. 创建 CONTRIBUTING.md (推荐)
- **内容**: 说明如何提交 Bug、如何贡献代码、Commit Message 的规范。

---

## 三、 代码仓库清理 🧹

### 1. 完善 .gitignore
请参考 `docs/GITIGNORE_TEMPLATE.md` 确保以下内容不被追踪：
- **Java**: `target/`, `*.class`, `.mvn/wrapper/`。
- **前端**: `node_modules/`, `dist/`。
- **IDE**: `.idea/`, `.vscode/`, `*.iml`。

### 2. 代码规范检查
- **注释**: 确认新编写的功能模块包含清晰的中文注释。
- **硬编码**: 检查代码中是否包含硬编码的 IP 地址（如 `127.0.0.1` 建议通过配置读取）。

---

## 四、 GitHub 仓库配套设置 ⚙️

推送到 GitHub 后，请完成以下设置以提高项目专业度：

1. **About 区域**: 填入简短的英文介绍，并添加 Tags (如 `rag`, `llm`, `milvus`, `spring-boot`)。
2. **Issue Templates**: 配置 Bug Report 和 Feature Request 模板。
3. **Discussions**: 如果有较大社区计划，可以开启 Discussions。
4. **Wiki**: 对于复杂的架构文档（如 `docs/鉴权模块设计方案.md`），可以考虑搬运至 GitHub Wiki。

---

## 五、 开源前自检 Checklist ✅

- [ ] 所有 API Key (尤其是 Gemini/OpenAI) 已从代码中物理移除且已脱敏。
- [ ] 后端通过 `mvn clean package -DskipTests` 编译通过。
- [ ] 前端通过 `npm run build` 打包无误。
- [ ] 根目录包含 `README.md` 和 `LICENSE`。
- [ ] 提供的 `docker-compose.milvus.yml` 可以一键启动基础环境。

---
**GuideRAG 开源团队预祝项目发布顺利！**
