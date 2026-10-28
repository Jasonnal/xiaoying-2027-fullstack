import { NextResponse, type NextRequest } from "next/server";

/**
 * 前后端之间的接口中间层。
 *
 * 浏览器只访问同源的 `/api/*`，由 Next.js 在服务端转发到 Java：
 * - Cookie 同源，避免 CORS + 第三方 Cookie 的一堆坑；
 * - 后端地址是运行时读取的（BACKEND_URL），容器里改成 `http://backend:8080` 即可，不用重新构建；
 * - 后端不可用时返回 503，前端据此区分「请求异常」和「任务失败」。
 */

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

type RouteContext = { params: Promise<{ path: string[] }> };

const METHODS_WITHOUT_BODY = new Set(["GET", "HEAD"]);

async function forward(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const incoming = new URL(request.url);
  const target = `${BACKEND_URL}/api/${path.map(encodeURIComponent).join("/")}${incoming.search}`;

  const headers = new Headers(request.headers);
  for (const header of ["host", "connection", "content-length", "accept-encoding"]) {
    headers.delete(header);
  }

  const init: RequestInit & { duplex?: "half" } = {
    method: request.method,
    headers,
    redirect: "manual",
    cache: "no-store",
  };
  if (!METHODS_WITHOUT_BODY.has(request.method)) {
    init.body = request.body;
    init.duplex = "half";
  }

  let upstream: Response;
  try {
    upstream = await fetch(target, init);
  } catch {
    return NextResponse.json(
      { code: "UPSTREAM_UNAVAILABLE", message: "后端服务暂时不可用，请稍后重试" },
      { status: 503 },
    );
  }

  const responseHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    if (["content-encoding", "content-length", "transfer-encoding", "set-cookie"].includes(key)) {
      return;
    }
    responseHeaders.set(key, value);
  });
  // 一个响应可能带多个 Set-Cookie，必须逐个转发
  for (const cookie of upstream.headers.getSetCookie()) {
    responseHeaders.append("set-cookie", cookie);
  }

  return new NextResponse(upstream.body, {
    status: upstream.status,
    headers: responseHeaders,
  });
}

export {
  forward as GET,
  forward as POST,
  forward as PUT,
  forward as PATCH,
  forward as DELETE,
  forward as HEAD,
};
