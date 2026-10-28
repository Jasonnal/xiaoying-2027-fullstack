"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useDemoAccounts, useLogin } from "@/lib/hooks";
import type { DemoAccount, Me } from "@/lib/types";
import { NoticeBar, Spinner } from "./StateBlocks";

function Avatar({ name, color, size = 36 }: { name: string; color: string; size?: number }) {
  return (
    <span
      className="flex shrink-0 items-center justify-center rounded-full text-xs font-semibold text-ink-950"
      style={{ background: color, width: size, height: size }}
      aria-hidden="true"
    >
      {name.slice(0, 1)}
    </span>
  );
}

export function AccountChooser({ onPicked }: { onPicked?: (me: Me) => void }) {
  const accounts = useDemoAccounts();
  const login = useLogin();
  const [handle, setHandle] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = (nextHandle: string, displayName?: string) => {
    setError(null);
    login.mutate(
      { handle: nextHandle, displayName },
      {
        onSuccess: (me) => onPicked?.(me),
        onError: (err: ApiError) => setError(err.message),
      },
    );
  };

  return (
    <div className="w-full max-w-md space-y-5">
      <div>
        <p className="label">选择身份</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-chalk-100">进入素材工坊</h1>
        <p className="mt-2 text-sm text-chalk-400">
          演示用身份：无需密码。不同账号看到各自的任务与历史，可随时切换验证数据隔离。
        </p>
      </div>

      {error ? <NoticeBar tone="error" title="进入失败" description={error} /> : null}

      <div className="space-y-2">
        {accounts.isPending ? (
          <div className="flex items-center gap-2 text-sm text-chalk-400">
            <Spinner /> 正在读取演示账号…
          </div>
        ) : null}

        {accounts.data?.map((account: DemoAccount) => (
          <button
            key={account.handle}
            type="button"
            className="flex w-full items-center gap-3 rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 text-left transition hover:border-mint-500/40 hover:bg-mint-500/[0.06] disabled:opacity-60"
            disabled={login.isPending}
            onClick={() => submit(account.handle)}
          >
            <Avatar name={account.displayName} color={account.accentColor} />
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-medium text-chalk-100">{account.displayName}</span>
              <span className="block text-[11px] text-chalk-400">@{account.handle}</span>
            </span>
            <span className="text-xs text-chalk-400">{login.isPending ? "进入中…" : "进入"}</span>
          </button>
        ))}

        {accounts.isError ? (
          <NoticeBar
            tone="warn"
            title="演示账号加载失败"
            description="可以手动输入标识进入，接口恢复后会自动同步。"
          />
        ) : null}
      </div>

      <form
        className="space-y-2"
        onSubmit={(event) => {
          event.preventDefault();
          if (handle.trim().length >= 2) {
            submit(handle.trim());
          } else {
            setError("标识至少 2 个字符");
          }
        }}
      >
        <label className="label block" htmlFor="handle-input">
          或使用自定义标识
        </label>
        <div className="flex gap-2">
          <input
            id="handle-input"
            name="handle"
            value={handle}
            onChange={(event) => setHandle(event.target.value)}
            placeholder="例如 guest-01"
            className="min-w-0 flex-1 rounded-xl border border-white/10 bg-ink-900 px-3 py-2.5 text-sm text-chalk-100 placeholder:text-chalk-500 focus:border-mint-500/60 focus:outline-none"
            maxLength={32}
          />
          <button type="submit" className="btn-ghost" disabled={login.isPending}>
            进入
          </button>
        </div>
        <p className="text-[11px] text-chalk-500">同一标识会复用同一账号，换标识即换身份。</p>
      </form>
    </div>
  );
}

export function LoginGate() {
  return (
    <div className="flex min-h-dvh items-center justify-center px-5 py-12">
      <div className="grid w-full max-w-5xl items-center gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-6">
          <div className="flex items-center gap-3">
            <BrandMark />
            <div>
              <p className="text-sm font-semibold tracking-[0.2em] text-chalk-100">MOTION STUDIO</p>
              <p className="text-[11px] text-chalk-400">素材工坊 · 全栈演示</p>
            </div>
          </div>
          <h2 className="text-3xl font-semibold leading-tight tracking-tight text-chalk-100 sm:text-4xl">
            选模板，传素材，
            <br />
            剩下的交给后台。
          </h2>
          <ul className="space-y-2 text-sm text-chalk-300">
            {[
              "提交即返回任务 ID，制作在 Java 后台继续，关闭页面不中断",
              "三段流水线：素材检查 → 模拟生成 → 结果整理，阶段实时可见",
              "多任务并行、刷新恢复、账号间数据隔离",
            ].map((item) => (
              <li key={item} className="flex items-start gap-2">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-mint-400" />
                {item}
              </li>
            ))}
          </ul>
        </div>
        <div className="panel p-6 sm:p-8">
          <AccountChooser />
        </div>
      </div>
    </div>
  );
}

export function BrandMark({ size = 42 }: { size?: number }) {
  return (
    <span
      className="flex items-center justify-center rounded-2xl bg-gradient-to-br from-mint-400 to-mint-600 text-ink-950 shadow-pop"
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      <svg viewBox="0 0 24 24" width={size * 0.5} height={size * 0.5} fill="currentColor">
        <path d="M9 6.5v11l8.5-5.5L9 6.5Z" />
      </svg>
    </span>
  );
}
