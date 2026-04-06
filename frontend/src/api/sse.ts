import { useUserStore } from "@/stores/user";

/**
 * SSE 流式聊天工具
 *
 * 对接后端真实 SSE 端点：
 *   POST /rag/v1/sessions/{sessionId}/chat/stream
 *
 * 后端推送的 SSE 事件名（来自 ChatServiceImpl）：
 *   - citations  → JSON 数组，知识库引用块，在第一个 token 前推送
 *   - token      → 纯文本，每个 LLM 输出 token
 *   - metaData   → JSON 对象 {messageId, tokenInput, tokenOutput, done: true}，流结束时推送
 *   - error      → JSON 对象 {error: "..."} ，出错时推送
 */

/** SSE 事件类型 */
export type SSEEventType =
  | "start"
  | "delta"
  | "citation"
  | "ping"
  | "error"
  | "done";

/** start 事件数据（由 metaData 事件映射而来） */
export interface SSEStartData {
  messageId: string;
  sessionId: string;
  timestamp: string;
}

/** delta 事件数据 */
export interface SSEDeltaData {
  delta: string;
}

/** citation 事件数据 */
export interface SSECitationData {
  citations: Array<{
    docId: string;
    chunkId: string;
    score?: number;
    sourceText?: string;
  }>;
}

/** done 事件数据（由 metaData 事件映射而来） */
export interface SSEDoneData {
  usage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  finishReason: string;
}

/** error 事件数据 */
export interface SSEErrorData {
  code: string | number;
  message: string;
  detail?: string;
}

/** SSE 配置选项 */
export interface SSEOptions {
  /**
   * 自定义请求 URL（可选）
   * 默认自动根据 payload.sessionId 构造：
   *   /api/rag/v1/sessions/{sessionId}/chat/stream
   */
  url?: string;
  /** 请求体参数 */
  payload: {
    sessionId: string;
    /** 用户提问（发送给后端时映射为 question 字段） */
    query: string;
    kbId?: string;
    temperature?: number;
    enableRetrieval?: boolean;
    topK?: number;
    /** 自定义模型覆盖配置 */
    modelOverride?: {
      provider: string;
      apiKey: string;
      baseUrl: string;
      model: string;
    };
  };
  /** 流式 ID 确认回调（后端 metaData 事件返回时触发） */
  onStart?: (data: SSEStartData) => void;
  /** 文本增量回调（后端 token 事件） */
  onDelta?: (text: string) => void;
  /** 引用文档回调（后端 citations 事件） */
  onCitation?: (data: SSECitationData) => void;
  /** 流结束回调（后端 metaData.done = true 时触发） */
  onDone?: (data: SSEDoneData) => void;
  /** 错误回调（后端 error 事件） */
  onError?: (error: SSEErrorData) => void;
  /** 中止信号 */
  signal?: AbortSignal;
}

class SSEAbortError extends Error {
  constructor() {
    super("SSE connection aborted");
    this.name = "SSEAbortError";
  }
}

/**
 * 安全解析 JSON
 * 解析失败时返回 null，避免 try/catch 散落各处
 */
function safeParse<T = unknown>(raw: string): T | null {
  if (!raw || raw.trim() === "") return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

/**
 * 启动流式聊天
 *
 * 使用原生 Fetch + ReadableStream 实现 SSE，支持：
 * - POST 请求（EventSource 不支持 POST）
 * - 自定义请求头（Authorization）
 * - AbortController 取消
 */
export async function startChatStream(options: SSEOptions): Promise<void> {
  const userStore = useUserStore();

  // ✅ 正确的后端 URL：sessionId 在路径中
  const url =
    options.url ||
    `/api/rag/v1/sessions/${options.payload.sessionId}/chat/stream`;

  const controller = new AbortController();

  // 如果外部提供了 signal，级联中止
  if (options.signal) {
    options.signal.addEventListener("abort", () => controller.abort());
  }

  try {
    // ✅ 请求体只包含 ChatReqDTO 需要的字段，sessionId 在 URL 中不重复传
    const requestBody: Record<string, unknown> = {
      question: options.payload.query, // query → question
      temperature: options.payload.temperature ?? 0.5,
      enableRetrieval: options.payload.enableRetrieval ?? true,
      topK: options.payload.topK ?? 5,
    };

    // ✅ 如果指定了知识库 ID，传给后端以覆盖 session 默认绑定的知识库
    if (options.payload.kbId) {
      requestBody.kbId = options.payload.kbId;
    }

    // ✅ 如果指定了自定义模型配置，传给后端
    if (options.payload.modelOverride) {
      requestBody.modelOverride = options.payload.modelOverride;
    }

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
        ...(userStore.token
          ? { Authorization: `Bearer ${userStore.token}` }
          : {}),
      },
      body: JSON.stringify(requestBody),
      signal: controller.signal,
    });

    // 非 2xx：解析错误信息后回调
    if (!response.ok) {
      const errorText = await response.text();
      const errorData = safeParse<{ message?: string; error?: string }>(
        errorText,
      );
      options.onError?.({
        code: response.status.toString(),
        message:
          errorData?.message ||
          errorData?.error ||
          `SSE connection failed: ${response.status}`,
      });
      return;
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error("Response body is not readable");
    }

    const decoder = new TextDecoder();
    let buffer = "";
    let currentEvent = "";
    let dataBuffer: string | null = null;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() || "";

      for (const line of lines) {
        if (line === "") {
          // 遇到空行意味着一个完整的 SSE 事件包已经接收完毕
          if (dataBuffer !== null) {
            switch (currentEvent) {
              case "token": {
                // Spring 可能会将明文转义，例如如果是纯文本也会有双引号？
                const parsed = safeParse<string>(dataBuffer);
                const tokenText = typeof parsed === "string" ? parsed : dataBuffer;
                // token 可能是单纯的换行符 \n，此时也必须向外发送
                if (tokenText !== "") {
                  options.onDelta?.(tokenText);
                }
                break;
              }
              case "citations": {
                const cits = safeParse<SSECitationData["citations"]>(dataBuffer);
                if (Array.isArray(cits)) {
                  options.onCitation?.({ citations: cits });
                }
                break;
              }
              case "metaData": {
                const meta = safeParse<{
                  messageId: string | number;
                  tokenInput?: number;
                  tokenOutput?: number;
                  done?: boolean;
                }>(dataBuffer);

                if (meta) {
                  options.onStart?.({
                    messageId: String(meta.messageId),
                    sessionId: options.payload.sessionId,
                    timestamp: new Date().toISOString(),
                  });

                  if (meta.done) {
                    const tokenIn = meta.tokenInput ?? 0;
                    const tokenOut = meta.tokenOutput ?? 0;
                    options.onDone?.({
                      finishReason: "stop",
                      usage: {
                        promptTokens: tokenIn,
                        completionTokens: tokenOut,
                        totalTokens: tokenIn + tokenOut,
                      },
                    });
                  }
                }
                break;
              }
              case "error": {
                const errData = safeParse<{
                  error?: string;
                  message?: string;
                  code?: string | number;
                }>(dataBuffer);
                options.onError?.({
                  code: errData?.code ?? "error",
                  message: errData?.error ?? errData?.message ?? dataBuffer,
                });
                break;
              }
              case "ping":
                break;
              default:
                if (!currentEvent && dataBuffer !== null) {
                  const parsed = safeParse<{ delta?: string }>(dataBuffer);
                  if (parsed?.delta) {
                    options.onDelta?.(parsed.delta);
                  } else if (!parsed) {
                    if (dataBuffer !== "") {
                      options.onDelta?.(dataBuffer);
                    }
                  }
                }
                break;
            }
          }
          currentEvent = "";
          dataBuffer = null;
          continue;
        }

        if (line.startsWith("event:")) {
          currentEvent = line.slice(6).trim();
        } else if (line.startsWith("data:")) {
          let val = line.slice(5);
          if (val.startsWith(" ")) {
            val = val.substring(1);
          }
          if (dataBuffer !== null) {
            dataBuffer += "\n";
          } else {
            dataBuffer = "";
          }
          dataBuffer += val;
        }
      }
    }

    // 应对流被突然切断没有空行结尾的情况
    if (dataBuffer !== null && dataBuffer !== "") {
       if (currentEvent === "token" || !currentEvent) {
          const parsed = safeParse<string>(dataBuffer);
          const tokenText = typeof parsed === "string" ? parsed : dataBuffer;
          if (tokenText !== "") options.onDelta?.(tokenText);
       }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new SSEAbortError();
    }
    throw error;
  }
}

/**
 * 创建可取消的流式聊天
 *
 * @returns { promise, abort }
 *   - promise：流完成时 resolve
 *   - abort：调用后立即中止流
 */
export function createChatStream(options: Omit<SSEOptions, "signal">) {
  const controller = new AbortController();

  const promise = startChatStream({
    ...options,
    signal: controller.signal,
  });

  return {
    promise,
    abort: () => controller.abort(),
  };
}
