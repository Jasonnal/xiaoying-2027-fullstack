import { ApiError } from "./api";
import type { UploadedAsset } from "./types";

interface UploadOptions {
  onProgress?: (percent: number) => void;
  signal?: AbortSignal;
}

/**
 * 用 XHR 而不是 fetch：只有 XHR 能拿到上传进度，界面上才能显示"上传中 42%"。
 * 服务端返回的是可长期访问的地址，预览图不依赖浏览器临时 URL。
 */
export function uploadAsset(file: File, options: UploadOptions = {}): Promise<UploadedAsset> {
  return new Promise<UploadedAsset>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/assets", true);
    xhr.withCredentials = true;
    xhr.responseType = "text";

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && options.onProgress) {
        options.onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };

    xhr.onload = () => {
      const payload = parse(xhr.responseText);
      if (xhr.status >= 200 && xhr.status < 300 && payload) {
        resolve(payload as UploadedAsset);
        return;
      }
      const body = (payload ?? {}) as { code?: string; message?: string };
      reject(new ApiError(xhr.status, body.code ?? `HTTP_${xhr.status}`, body.message ?? "上传失败"));
    };

    xhr.onerror = () => reject(new ApiError(0, "NETWORK_ERROR", "上传过程中网络中断，请重试"));
    xhr.ontimeout = () => reject(new ApiError(0, "NETWORK_ERROR", "上传超时，请重试"));
    xhr.onabort = () => reject(new DOMException("上传已取消", "AbortError"));

    if (options.signal) {
      if (options.signal.aborted) {
        xhr.abort();
      } else {
        options.signal.addEventListener("abort", () => xhr.abort(), { once: true });
      }
    }

    const form = new FormData();
    form.append("file", file, file.name);
    xhr.send(form);
  });
}

function parse(text: string): unknown {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
