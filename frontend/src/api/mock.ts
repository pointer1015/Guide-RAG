/**
 * 模拟数据服务
 * 用于开发环境的数据模拟
 */
import type {
  UserProfile,
  Session,
  Message,
  KnowledgeBase,
  Document
} from '@/types'

// 延迟函数，模拟网络请求
export const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// 生成 UUID
export const generateId = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// 模拟用户数据
export const mockUsers: Record<string, { password: string; profile: UserProfile }> = {
  admin: {
    password: 'admin123',
    profile: {
      id: 'user-001',
      username: 'admin',
      email: 'admin@example.com',
      nickname: '管理员',
      avatar: '',
      createdAt: '2026-01-01T00:00:00Z'
    }
  },
  demo: {
    password: 'demo123',
    profile: {
      id: 'user-002',
      username: 'demo',
      email: 'demo@example.com',
      nickname: '演示用户',
      avatar: '',
      createdAt: '2026-03-01T00:00:00Z'
    }
  }
}

// 模拟会话数据
export const mockSessions: Session[] = [
  {
    id: 'session-001',
    title: '关于公司年度报告的问答',
    knowledgeBaseId: 'kb-001',
    createdAt: '2026-03-30T10:00:00Z',
    updatedAt: '2026-03-30T14:30:00Z'
  },
  {
    id: 'session-002',
    title: '产品使用手册查询',
    knowledgeBaseId: 'kb-002',
    createdAt: '2026-03-29T09:00:00Z',
    updatedAt: '2026-03-29T16:00:00Z'
  },
  {
    id: 'session-003',
    title: '技术文档检索',
    knowledgeBaseId: 'kb-001',
    createdAt: '2026-03-28T14:00:00Z',
    updatedAt: '2026-03-28T18:00:00Z'
  }
]

// 模拟消息数据
export const mockMessages: Record<string, Message[]> = {
  'session-001': [
    {
      id: 'msg-001',
      sessionId: 'session-001',
      role: 'user',
      content: '公司2025年的营收情况如何？',
      createdAt: '2026-03-30T10:00:00Z'
    },
    {
      id: 'msg-002',
      sessionId: 'session-001',
      role: 'assistant',
      content: '根据公司年度报告显示，2025年全年营收达到 **50亿元**，同比增长 **25%**。主要增长来源于：\n\n1. **核心业务收入**：35亿元，占比70%\n2. **新兴业务收入**：10亿元，占比20%\n3. **其他收入**：5亿元，占比10%\n\n毛利率维持在 **45%** 的健康水平。',
      sources: [
        { title: '2025年度财务报告.pdf', chunkId: 'chunk-001', content: '2025年全年营收50亿元...' },
        { title: '季度分析报告.docx', chunkId: 'chunk-002', content: '核心业务保持稳健增长...' }
      ],
      createdAt: '2026-03-30T10:00:05Z'
    },
    {
      id: 'msg-003',
      sessionId: 'session-001',
      role: 'user',
      content: '利润率相比去年有什么变化？',
      createdAt: '2026-03-30T10:05:00Z'
    },
    {
      id: 'msg-004',
      sessionId: 'session-001',
      role: 'assistant',
      content: '2025年的净利润率为 **18%**，相比2024年的 **15%** 提升了 **3个百分点**。利润率提升的主要原因包括：\n\n- 运营效率提升，人均产出增加 20%\n- 供应链优化，采购成本降低 8%\n- 数字化转型成效显著，IT成本占比下降\n\n```\n利润率趋势：\n2023年: 12%\n2024年: 15%\n2025年: 18%\n```',
      sources: [
        { title: '2025年度财务报告.pdf', chunkId: 'chunk-003' }
      ],
      createdAt: '2026-03-30T10:05:08Z'
    }
  ],
  'session-002': [
    {
      id: 'msg-005',
      sessionId: 'session-002',
      role: 'user',
      content: '如何配置系统的邮件通知功能？',
      createdAt: '2026-03-29T09:00:00Z'
    },
    {
      id: 'msg-006',
      sessionId: 'session-002',
      role: 'assistant',
      content: '配置邮件通知功能，请按以下步骤操作：\n\n## 1. 进入设置页面\n\n点击右上角的 **设置** 图标，选择 **通知管理**。\n\n## 2. 配置 SMTP 服务器\n\n```yaml\nsmtp:\n  host: smtp.example.com\n  port: 587\n  username: your-email@example.com\n  password: your-password\n  encryption: tls\n```\n\n## 3. 设置通知规则\n\n- 选择需要接收通知的事件类型\n- 配置接收人邮箱列表\n- 设置通知频率（实时/每日汇总）\n\n## 4. 测试发送\n\n点击 **发送测试邮件** 验证配置是否正确。',
      sources: [
        { title: '系统管理员手册.pdf', chunkId: 'chunk-010' }
      ],
      createdAt: '2026-03-29T09:00:10Z'
    }
  ],
  'session-003': []
}

// 模拟知识库数据
export const mockKnowledgeBases: KnowledgeBase[] = [
  {
    id: 'kb-001',
    name: '企业文档库',
    description: '包含公司年度报告、财务分析、战略规划等核心文档',
    documentCount: 15,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-03-30T12:00:00Z'
  },
  {
    id: 'kb-002',
    name: '产品知识库',
    description: '产品使用手册、技术规格、常见问题解答',
    documentCount: 28,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-03-28T16:00:00Z'
  },
  {
    id: 'kb-003',
    name: '技术文档库',
    description: 'API文档、架构设计、开发指南',
    documentCount: 42,
    createdAt: '2026-02-20T00:00:00Z',
    updatedAt: '2026-03-25T10:00:00Z'
  }
]

// 模拟文档数据
export const mockDocuments: Record<string, Document[]> = {
  'kb-001': [
    {
      id: 'doc-001',
      knowledgeBaseId: 'kb-001',
      name: '2025年度财务报告.pdf',
      size: 2548000,
      type: 'application/pdf',
      status: 'PARSED',
      progress: 100,
      createdAt: '2026-03-01T10:00:00Z',
      updatedAt: '2026-03-01T10:05:00Z'
    },
    {
      id: 'doc-002',
      knowledgeBaseId: 'kb-001',
      name: '季度分析报告.docx',
      size: 1024000,
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      status: 'PARSED',
      progress: 100,
      createdAt: '2026-03-15T14:00:00Z',
      updatedAt: '2026-03-15T14:03:00Z'
    },
    {
      id: 'doc-003',
      knowledgeBaseId: 'kb-001',
      name: '战略规划2026.pdf',
      size: 3200000,
      type: 'application/pdf',
      status: 'PARSING',
      progress: 65,
      createdAt: '2026-03-30T09:00:00Z',
      updatedAt: '2026-03-30T09:02:00Z'
    },
    {
      id: 'doc-004',
      knowledgeBaseId: 'kb-001',
      name: '市场调研报告.pdf',
      size: 1800000,
      type: 'application/pdf',
      status: 'PENDING',
      progress: 0,
      createdAt: '2026-03-30T11:00:00Z',
      updatedAt: '2026-03-30T11:00:00Z'
    }
  ],
  'kb-002': [
    {
      id: 'doc-005',
      knowledgeBaseId: 'kb-002',
      name: '系统管理员手册.pdf',
      size: 4500000,
      type: 'application/pdf',
      status: 'PARSED',
      progress: 100,
      createdAt: '2026-02-10T10:00:00Z',
      updatedAt: '2026-02-10T10:10:00Z'
    },
    {
      id: 'doc-006',
      knowledgeBaseId: 'kb-002',
      name: '用户指南.md',
      size: 256000,
      type: 'text/markdown',
      status: 'PARSED',
      progress: 100,
      createdAt: '2026-02-15T09:00:00Z',
      updatedAt: '2026-02-15T09:01:00Z'
    }
  ],
  'kb-003': [
    {
      id: 'doc-007',
      knowledgeBaseId: 'kb-003',
      name: 'API文档v2.0.md',
      size: 512000,
      type: 'text/markdown',
      status: 'PARSED',
      progress: 100,
      createdAt: '2026-03-01T08:00:00Z',
      updatedAt: '2026-03-01T08:02:00Z'
    },
    {
      id: 'doc-008',
      knowledgeBaseId: 'kb-003',
      name: '架构设计文档.pdf',
      size: 2800000,
      type: 'application/pdf',
      status: 'FAILED',
      progress: 30,
      errorMessage: '文档格式不支持，请检查文件是否损坏',
      createdAt: '2026-03-20T16:00:00Z',
      updatedAt: '2026-03-20T16:05:00Z'
    }
  ]
}

// 模拟流式响应的文本
export const mockStreamResponses = [
  '根据您的问题，我来为您分析一下相关信息。\n\n',
  '## 核心要点\n\n',
  '1. **第一点**：这是从知识库中检索到的关键信息，',
  '涉及到业务的核心逻辑和实现细节。\n\n',
  '2. **第二点**：基于文档分析，我们可以得出以下结论，',
  '这对于理解整体架构非常重要。\n\n',
  '3. **第三点**：补充说明一些注意事项和最佳实践。\n\n',
  '## 代码示例\n\n',
  '```typescript\n',
  'const result = await api.query({\n',
  '  question: "your question",\n',
  '  knowledgeBase: "kb-001"\n',
  '});\n',
  '```\n\n',
  '希望以上信息对您有所帮助！如有其他问题，请随时提问。'
]
