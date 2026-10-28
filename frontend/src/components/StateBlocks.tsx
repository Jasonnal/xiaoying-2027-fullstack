import { cn } from "@/lib/format";

export function Spinner({ className }: { className?: string }) {
  return (
    <svg
      className={cn("h-4 w-4 animate-spin", className)}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity="0.2" strokeWidth="3" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export function CardSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="panel overflow-hidden">
          <div className="relative aspect-[16/10] overflow-hidden bg-ink-800">
            <div className="absolute inset-0 -translate-x-full animate-sweep bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          </div>
          <div className="space-y-3 p-4">
            <div className="h-4 w-2/3 rounded bg-white/10" />
            <div className="h-3 w-1/2 rounded bg-white/5" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="panel flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.04] text-chalk-400">
        <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.6">
          <rect x="3.5" y="4.5" width="17" height="15" rx="3" />
          <path d="M3.5 15.5l4.6-4.2 3.4 3 3-2.6 5 4.3" strokeLinecap="round" />
          <circle cx="9" cy="9.4" r="1.4" />
        </svg>
      </div>
      <p className="text-base font-medium text-chalk-100">{title}</p>
      {description ? <p className="max-w-md text-sm text-chalk-400">{description}</p> : null}
      {action}
    </div>
  );
}

export function ErrorState({
  title = "加载失败",
  message,
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="panel flex flex-col items-center justify-center gap-3 px-6 py-14 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-coral/30 bg-coral/10 text-coral">
        <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8">
          <path d="M12 8v5" strokeLinecap="round" />
          <circle cx="12" cy="16.4" r="0.9" fill="currentColor" stroke="none" />
          <path d="M10.3 4.3 3.5 17a2 2 0 0 0 1.8 3h13.4a2 2 0 0 0 1.8-3L13.7 4.3a2 2 0 0 0-3.4 0Z" />
        </svg>
      </div>
      <p className="text-base font-medium text-chalk-100">{title}</p>
      {message ? <p className="max-w-md text-sm text-chalk-400">{message}</p> : null}
      {onRetry ? (
        <button type="button" className="btn-ghost mt-1" onClick={onRetry}>
          重新加载
        </button>
      ) : null}
    </div>
  );
}

/** 顶部提示条：用于"状态刷新失败"这类不阻断操作的异常。 */
export function NoticeBar({
  tone = "warn",
  title,
  description,
  action,
}: {
  tone?: "warn" | "error" | "success" | "info";
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  const tones: Record<string, string> = {
    warn: "border-ember/30 bg-ember/10 text-ember",
    error: "border-coral/30 bg-coral/10 text-coral",
    success: "border-mint-500/30 bg-mint-500/10 text-mint-300",
    info: "border-white/15 bg-white/[0.04] text-chalk-200",
  };

  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={cn("flex flex-wrap items-center gap-3 rounded-xl border px-4 py-3 text-sm", tones[tone])}
    >
      <div className="flex-1">
        <p className="font-medium">{title}</p>
        {description ? <p className="mt-0.5 text-xs opacity-80">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}
