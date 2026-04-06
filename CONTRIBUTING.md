# 贡献指南 (Contributing Guide)

感谢您对 **GuideRAG** 项目的关注！我们欢迎任何形式的贡献，包括修复 Bug、增加新功能、改进文档或提出建议。

## 1. 准备工作

在开始贡献之前，请确保您的开发环境满足以下要求：

- **JDK 21+**
- **Node.js 20+**
- **Maven 3.9+**
- **Docker & Docker Compose** (用于运行 Milvus, MinIO 等中间件)
- **MySQL 8.0+** 或 **PostgreSQL**

## 2. 开发流程

1. **Fork 本仓库** 到您自己的 GitHub 账号。
2. **克隆仓库** 到本地：
   ```bash
   git clone https://github.com/your-username/guide-rag.git
   ```
3. **创建特性分支**：
   ```bash
   git checkout -b feature/your-feature-name
   # 或者修复 Bug
   git checkout -b fix/your-bug-name
   ```
4. **进行开发**：
   - 后端代码请遵循通用的 Java 代码规范。
   - 前端代码推荐使用 Prettier 进行格式化。
5. **提交代码**：
   请遵循 [Conventional Commits](#3-commit-规范) 规范。
6. **推送分支**：
   ```bash
   git push origin feature/your-feature-name
   ```
7. **开启 Pull Request (PR)**：
   在 GitHub 上发起向 `main` 分支的 PR。

## 3. Commit 规范

我们采用 **Conventional Commits** 规范，提交格式如下：

```
<type>(<scope>): <description>
```

常见的 `type` 包括：
- `feat`: 新功能 (feature)
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 格式调整 (不影响代码逻辑的变动)
- `refactor`: 重构 (既不修复 Bug 也不添加新功能的代码变动)
- `perf`: 性能优化
- `test`: 增加测试
- `chore`: 构建过程或辅助工具的变动

**示例**：
- `feat(chat): 增加流式响应处理`
- `fix(auth): 修复 Token 过期校验逻辑`

## 4. 代码风格

- **后端**：使用标准 Maven 结构，包名以 `com.guiderag` 开头。
- **前端**：使用 Vue 3 组合式 API (Setup)，遵循 TypeScript 规范。

## 5. 问题反馈

如果您发现了 Bug 或有新想法，请在 GitHub Issues 中进行反馈。在提交 Issue 前，请搜索是否已有类似的问题。

感谢您的贡献！
