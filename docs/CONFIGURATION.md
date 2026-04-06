# 配置文件说明

## 环境变量配置

为了保护敏感信息，项目使用环境变量来配置关键参数。请在运行前设置以下环境变量：

### 数据库配置
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_database_password
```

### Redis 配置
```bash
export REDIS_PASSWORD=your_redis_password  # 如果 Redis 没有密码则不设置
```

### MinIO 对象存储配置
```bash
export MINIO_ACCESS_KEY=your_minio_access_key
export MINIO_SECRET_KEY=your_minio_secret_key
```

### LLM API 密钥配置（按需配置）
```bash
# DeepSeek API
export DEEPSEEK_API_KEY=sk-your-deepseek-api-key

# OpenAI API
export OPENAI_API_KEY=sk-your-openai-api-key

# Gemini API
export GEMINI_API_KEY=your-gemini-api-key

# OpenRouter API
export OPENROUTER_API_KEY=your-openrouter-api-key
```

## Windows 系统环境变量设置

在 PowerShell 中：
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
```

或永久设置：
```powershell
[System.Environment]::SetEnvironmentVariable('DB_USERNAME', 'postgres', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_password', 'User')
```

## Linux/macOS 系统环境变量设置

在终端中：
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

或在 `~/.bashrc` 或 `~/.zshrc` 中添加：
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

## 配置文件示例

项目提供了以下示例配置文件：
- `rag-auth/src/main/resources/application-dev.yml.example`
- `rag-chat/src/main/resources/application-dev.yml.example`

首次部署时，请参考示例文件配置您的 `application-dev.yml`。

## 注意事项

⚠️ **重要：永远不要将包含真实密码/密钥的配置文件提交到版本控制系统！**

- `application-local.yml` 已在 `.gitignore` 中排除
- 生产环境建议使用配置中心（如 Nacos）管理敏感配置
- 开发环境可使用环境变量或 `application-local.yml`（不会被提交）
