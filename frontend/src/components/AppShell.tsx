"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { AccountChooser, BrandMark, LoginGate } from "./AccountChooser";
import { Spinner } from "./StateBlocks";
import { useLogout, useMe } from "@/lib/hooks";
import { cn } from "@/lib/format";
import type { Me } from "@/lib/types";

const NAV = [
  { href: "/", label: "模板" },
  { href: "/history", label: "历史记录" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const me = useMe();
  const [switching, setSwitching] = useState(false);

  if (me.isPending) {
    return (
      <div className="flex min-h-dvh items-center justify-center gap-3 text-sm text-chalk-400">
        <Spinner /> 正在恢复会话…
      </div>
    );
  }

  // 401 或被清空会话：回到身份选择页（不渲染业务页面，避免残留上一个账号的数据）
  if (!me.data) {
    return <LoginGate />;
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-30 border-b border-white/10 bg-ink-950/80 backdrop-blur">
        <div className="mx-auto flex w-full max-w-6xl items-center gap-3 px-5 py-3">
          <Link href="/" className="flex items-center gap-2.5" aria-label="回到模板列表">
            <BrandMark size={34} />
            <span className="hidden sm:block">
              <span className="block text-[13px] font-semibold tracking-[0.18em] text-chalk-100">
                MOTION STUDIO
              </span>
              <span className="block text-[10px] text-chalk-400">素材工坊</span>
            </span>
          </Link>

          <nav className="ml-3 flex items-center gap-1" aria-label="主导航">
            {NAV.map((item) => (
              <NavLink key={item.href} href={item.href} label={item.label} />
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <AccountPanel me={me.data} onSwitch={() => setSwitching(true)} />
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-5 pb-20 pt-6">{children}</main>

      <footer className="border-t border-white/5 py-6 text-center text-[11px] text-chalk-500">
        前端 Next.js App Router · 后端 Spring Boot + MyBatis + MySQL · 演示数据仅用于笔试题验收
      </footer>

      {switching ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/80 p-4 backdrop-blur-sm"
          role="dialog"
          aria-modal="true"
          aria-label="切换账号"
          onClick={() => setSwitching(false)}
        >
          <div
            className="panel max-h-[90vh] w-full max-w-md overflow-y-auto p-6 animate-rise"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <p className="text-sm font-medium text-chalk-100">切换 / 登录账号</p>
              <button
                type="button"
                className="btn-ghost !px-2.5 !py-1 !text-xs"
                onClick={() => setSwitching(false)}
              >
                关闭
              </button>
            </div>
            <AccountChooser onPicked={() => setSwitching(false)} />
          </div>
        </div>
      ) : null}
    </div>
  );
}

function NavLink({ href, label }: { href: string; label: string }) {
  const pathname = usePathname();
  const active = href === "/" ? pathname === "/" : pathname.startsWith(href);
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "rounded-lg px-3 py-1.5 text-sm transition",
        active ? "bg-white/[0.08] text-chalk-100" : "text-chalk-400 hover:bg-white/[0.05] hover:text-chalk-200",
      )}
    >
      {label}
    </Link>
  );
}

function AccountPanel({ me, onSwitch }: { me: Me; onSwitch: () => void }) {
  const [open, setOpen] = useState(false);
  const logout = useLogout();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onPointerDown = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.03] px-2 py-1.5 text-sm text-chalk-200 transition hover:border-white/20 hover:bg-white/[0.06]"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <span
          className="flex h-6 w-6 items-center justify-center rounded-full text-[11px] font-semibold text-ink-950"
          style={{ background: me.accentColor }}
          aria-hidden="true"
        >
          {me.displayName.slice(0, 1)}
        </span>
        <span className="hidden max-w-[120px] truncate sm:block">{me.displayName}</span>
        <svg viewBox="0 0 24 24" className="h-3.5 w-3.5 text-chalk-400" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="m6 9 6 6 6-6" strokeLinecap="round" />
        </svg>
      </button>

      {open ? (
        <div
          role="menu"
          className="panel absolute right-0 mt-2 w-56 overflow-hidden p-1.5 animate-rise"
        >
          <div className="px-3 py-2">
            <p className="text-sm text-chalk-100">{me.displayName}</p>
            <p className="text-[11px] text-chalk-400">@{me.handle}</p>
          </div>
          <div className="my-1 h-px bg-white/10" />
          <button
            type="button"
            role="menuitem"
            className="w-full rounded-lg px-3 py-2 text-left text-sm text-chalk-200 hover:bg-white/[0.06]"
            onClick={() => {
              setOpen(false);
              onSwitch();
            }}
          >
            切换账号
          </button>
          <button
            type="button"
            role="menuitem"
            className="w-full rounded-lg px-3 py-2 text-left text-sm text-coral hover:bg-coral/10"
            onClick={() => {
              setOpen(false);
              logout.mutate();
            }}
          >
            退出登录
          </button>
        </div>
      ) : null}
    </div>
  );
}
