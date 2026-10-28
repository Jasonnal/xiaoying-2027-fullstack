"use client";

import { useState } from "react";
import { JobCard } from "./JobCard";
import { ResultLightbox } from "./ResultLightbox";
import { CardSkeleton, EmptyState, ErrorState, NoticeBar, Spinner } from "./StateBlocks";
import { cn } from "@/lib/format";
import { countActive } from "@/lib/jobs";
import type { ApiError } from "@/lib/api";
import type { Job } from "@/lib/types";

interface JobListProps {
  jobs: Job[] | undefined;
  isLoading: boolean;
  error: ApiError | null;
  onRetry: () => void;
  isFetching?: boolean;
  compact?: boolean;
  emptyTitle?: string;
  emptyHint?: string;
  title?: string;
  className?: string;
}

/**
 * 任务列表。轮询由上层 useJobs 统一负责，这里只做渲染与状态区分：
 * - 请求异常（error && 有旧数据）：只提示"刷新失败"，已有任务状态保持不动；
 * - 从未拿到数据（error && 无数据）：整块错误状态 + 重试。
 */
export function JobList({
  jobs,
  isLoading,
  error,
  onRetry,
  isFetching = false,
  compact = false,
  emptyTitle = "还没有制作任务",
  emptyHint = "挑一个模板、上传素材，任务会出现在这里并实时刷新。",
  title = "制作任务",
  className,
}: JobListProps) {
  const [preview, setPreview] = useState<{ url: string; title: string } | null>(null);
  const active = countActive(jobs);

  return (
    <section className={cn("space-y-4", className)} aria-labelledby="job-list-title">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h2 id="job-list-title" className="text-sm font-semibold text-chalk-100">
            {title}
          </h2>
          {active > 0 ? (
            <span className="chip !border-mint-500/30 !bg-mint-500/10 !text-mint-300">
              <Spinner className="h-3 w-3" />
              {active} 个进行中
            </span>
          ) : null}
        </div>
        {isFetching && active > 0 ? (
          <span className="text-[11px] text-chalk-400">状态自动刷新中…</span>
        ) : null}
      </header>

      {error ? (
        <NoticeBar
          tone="warn"
          title="状态刷新失败，任务仍在后台执行"
          description={`${error.message}。已保留最后一次成功获取的状态，可手动重试。`}
          action={
            <button type="button" className="btn-ghost !py-1.5 !text-xs" onClick={onRetry}>
              立即重试
            </button>
          }
        />
      ) : null}

      {isLoading && !jobs ? (
        compact ? (
          <div className="space-y-3">
            <div className="h-24 animate-pulse rounded-xl bg-white/5" />
            <div className="h-24 animate-pulse rounded-xl bg-white/5" />
          </div>
        ) : (
          <CardSkeleton count={3} />
        )
      ) : null}

      {!isLoading && error && !jobs ? <ErrorState message={error.message} onRetry={onRetry} /> : null}

      {!isLoading && !error && jobs && jobs.length === 0 ? (
        <EmptyState title={emptyTitle} description={emptyHint} />
      ) : null}

      {jobs && jobs.length > 0 ? (
        <div
          className={cn(
            "grid gap-3",
            compact ? "grid-cols-1" : "grid-cols-1 sm:grid-cols-2 xl:grid-cols-3",
          )}
        >
          {jobs.map((job) => (
            <JobCard
              key={job.id}
              job={job}
              compact={compact}
              onPreview={(url) => setPreview({ url, title: job.templateTitle })}
            />
          ))}
        </div>
      ) : null}

      {preview ? (
        <ResultLightbox url={preview.url} title={preview.title} onClose={() => setPreview(null)} />
      ) : null}
    </section>
  );
}
