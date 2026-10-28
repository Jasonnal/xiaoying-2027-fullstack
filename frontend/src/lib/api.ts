import type { ApiErrorBody } from "./types";

/** 带 code 的接口异常：UI 只依赖 code 判断分支，message 直接展示。 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }

  /** 未登录（会话失效）——用来决定是否弹出账号选择。 */
  get isUnauthorized() {
    return this.status === 401;
  }

  /** 后端不可达 / 网关错误：属于"请求异常"，不能据此判定任务失败。 */
  get isTransport() {
    return this.status === 503 || this.status >= 500 || this.code === "NETWORK_ERROR";
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      credentials: "same-origin",
      cache: "no-store",
      ...init,
      headers: {
        Accept: "application/json",
        ...(init.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
        ...init.headers,
      },
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new ApiError(0, "NETWORK_ERROR", "网络连接中断，请检查网络后重试");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload = text ? safeParse(text) : null;

  if (!response.ok) {
    const body = (payload ?? {}) as ApiErrorBody;
    throw new ApiError(response.status, body.code ?? `HTTP_${response.status}`, body.message ?? "请求失败");
  }

  return payload as T;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
