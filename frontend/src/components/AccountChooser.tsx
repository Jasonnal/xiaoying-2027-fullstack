"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useDemoAccounts, useLogin, useRegister } from "@/lib/hooks";
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

type Mode = "login" | "register";

export function AccountChooser({ onPicked }: { onPicked?: (me: Me) => void }) {
  const accounts = useDemoAccounts();
  const login = useLogin();
  const register = useRegister();

  const [mode, setMode] = useState<Mode>("login");
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const busy = login.isPending || register.isPending;
  const switchMode = (next: Mode) => {
    setMode(next);
    setError(null);
  };

  const finish = (me: Me) => onPicked?.(me);
  const fail = (err: ApiError) => setError(err.message);

  const submitLogin = (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!handle.trim() || !password) {
      setError("请输入账号和密码");
      return;
    }
    login.mutate({ handle: handle.trim(), password }, { onSuccess: finish, onError: fail });
  };

  const submitRegister = (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!handle.trim() || !password) {
      setError("请输入账号和密码");
      return;
    }
    if (password.length < 6) {
      setError("密码至少 6 位");
      return;
    }
    register.mutate(
      { handle: handle.trim(), password, displayName: displayName.trim() || undefined },
      { onSuccess: finish, onError: fail },
    );
  };

  /** 演示账号一键登录：口令由 /api/auth/accounts 下发的演示值，不写死在前端。 */
  const quickLogin = (account: DemoAccount) => {
    setError(null);
    setHandle(account.handle);
    setPassword(account.password);
    login.mutate({ handle: account.handle, password: account.password }, { onSuccess: finish, onError: fail });
  };

  return (
    <div className="w-full max-w-md space-y-4">
      <div>
        <p className="label">账号登录</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-chalk-100">
          {mode === "login" ? "进入素材工坊" : "创建账号"}
        </h1>
        <p className="mt-1.5 text-sm text-chalk-400">
          {mode === "login"
            ? "账号 + 密码登录；下方演示账号可一键进入（口令 demo1234）。"
            : "注册后自动登录；账号用于隔离任务与历史。"}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-1 rounded-xl border border-white/10 bg-white/[0.03] p-1">
        {(
          [
            { key: "login", label: "登录" },
            { key: "register", label: "注册" },
          ] as const
        ).map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => switchMode(tab.key)}
            aria-pressed={mode === tab.key}
            className={
              mode === tab.key
                ? "rounded-lg bg-mint-500/15 py-2 text-sm font-medium text-mint-300"
                : "rounded-lg py-2 text-sm text-chalk-400 transition hover:text-chalk-200"
            }
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error ? <NoticeBar tone="error" title="操作失败" description={error} /> : null}

      <form className="space-y-3" onSubmit={mode === "login" ? submitLogin : submitRegister}>
        <div className="space-y-1.5">
          <label className="label block" htmlFor="handle-input">
            账号
          </label>
          <input
            id="handle-input"
            name="handle"
            value={handle}
            onChange={(event) => setHandle(event.target.value)}
            placeholder="例如 demo"
            autoComplete="username"
            className="w-full rounded-xl border border-white/10 bg-ink-900 px-3 py-2.5 text-sm text-chalk-100 placeholder:text-chalk-500 focus:border-mint-500/60 focus:outline-none"
            maxLength={32}
            required
          />
        </div>

        {mode === "register" ? (
          <div className="space-y-1.5">
            <label className="label block" htmlFor="display-name-input">
              昵称（可选）
            </label>
            <input
              id="display-name-input"
              name="displayName"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="显示在右上角的名字"
              maxLength={32}
              className="w-full rounded-xl border border-white/10 bg-ink-900 px-3 py-2.5 text-sm text-chalk-100 placeholder:text-chalk-500 focus:border-mint-500/60 focus:outline-none"
            />
          </div>
        ) : null}

        <div className="space-y-1.5">
          <label className="label block" htmlFor="password-input">
            密码
          </label>
          <input
            id="password-input"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={mode === "register" ? "至少 6 位" : "输入密码"}
            autoComplete={mode === "register" ? "new-password" : "current-password"}
            className="w-full rounded-xl border border-white/10 bg-ink-900 px-3 py-2.5 text-sm text-chalk-100 placeholder:text-chalk-500 focus:border-mint-500/60 focus:outline-none"
            maxLength={64}
            required
          />
        </div>

        <button type="submit" className="btn-primary w-full" disabled={busy} aria-busy={busy}>
          {busy ? <Spinner /> : null}
          {mode === "login" ? (busy ? "登录中…" : "登录") : busy ? "注册中…" : "注册并登录"}
        </button>
      </form>

      <div className="space-y-2">
        <div className="flex items-center gap-3">
          <span className="h-px flex-1 bg-white/10" />
          <span className="text-[11px] text-chalk-500">或使用演示账号（口令 demo1234）</span>
          <span className="h-px flex-1 bg-white/10" />
        </div>

        {accounts.isPending ? (
          <div className="flex items-center gap-2 text-sm text-chalk-400">
            <Spinner /> 正在读取演示账号…
          </div>
        ) : null}

        <div className="space-y-1.5">
          {accounts.data?.map((account) => (
            <button
              key={account.handle}
              type="button"
              className="flex w-full items-center gap-3 rounded-xl border border-white/10 bg-white/[0.03] px-3.5 py-2 text-left transition hover:border-mint-500/40 hover:bg-mint-500/[0.06] disabled:opacity-60"
              disabled={busy}
              onClick={() => quickLogin(account)}
            >
              <Avatar name={account.displayName} color={account.accentColor} size={30} />
              <span className="min-w-0 flex-1">
                <span className="block text-sm font-medium text-chalk-100">{account.displayName}</span>
                <span className="block text-[11px] text-chalk-400">@{account.handle}</span>
              </span>
              <span className="text-xs text-chalk-400">一键登录</span>
            </button>
          ))}
        </div>

        {accounts.isError ? (
          <NoticeBar tone="warn" title="演示账号加载失败" description="可直接用上方表单登录，接口恢复后会自动可用。" />
        ) : null}
      </div>
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
