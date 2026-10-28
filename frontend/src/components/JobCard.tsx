import { JobStatusPill } from "./JobStatusPill";
import { cn, formatDateTime, formatRelative } from "@/lib/format";
import type { Job, JobStage } from "@/lib/types";

const STEPS = [
  { stage: "MATERIAL_CHECK" as JobStage, label: "素材检查" },
  { stage: "GENERATE" as JobStage, label: "模拟生成" },
  { stage: "FINALIZE" as JobStage, label: "结果整理" },
];

const STAGE_ORDER: Record<JobStage, number> = {
  QUEUED: -1,
  MATERIAL_CHECK: 0,
  GENERATE: 1,
  FINALIZE: 2,
  DONE: 3,
};

const STAGE_LABEL: Record<string, string> = {
  QUEUED: "排队",
  MATERIAL_CHECK: "素材检查",
  GENERATE: "模拟生成",
  FINALIZE: "结果整理",
  DONE: "已完成",
};

export function JobCard({
  job,
  compact = false,
  onPreview,
}: {
  job: Job;
  compact?: boolean;
  onPreview?: (url: string, job: Job) => void;
}) {
  const thumbnail = job.resultUrl ?? job.assets[0]?.url ?? null;
  const pending = job.status === "PENDING" || job.status === "PROCESSING";

  return (
    <article
      className={cn("panel group overflow-hidden transition hover:border-white/20", compact && "rounded-xl")}
      aria-label={`任务 ${job.templateTitle}`}
    >
      <div className={cn("relative overflow-hidden bg-ink-800", compact ? "aspect-[16/9]" : "aspect-[16/10]")}>
        {thumbnail ? (
          // 缩略图来自后端静态接口，不是浏览器临时地址
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={thumbnail}
            alt={`${job.templateTitle} 预览`}
            className={cn(
              "h-full w-full object-cover transition duration-500",
              job.status === "FAILED" ? "opacity-35 grayscale" : "hover:scale-[1.03]",
            )}
          />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-chalk-400">暂无预览</div>
        )}

        <div className="absolute left-3 top-3">
          <JobStatusPill status={job.status} stageLabel={job.stageLabel} />
        </div>

        {job.status === "SUCCEEDED" && job.resultUrl ? (
          <button
            type="button"
            onClick={() => job.resultUrl && onPreview?.(job.resultUrl, job)}
            className="absolute inset-x-3 bottom-3 rounded-lg bg-ink-950/80 px-3 py-2 text-xs font-medium text-chalk-100 backdrop-blur transition hover:bg-ink-950 focus-visible:opacity-100 md:opacity-0 md:group-hover:opacity-100"
          >
            查看制作结果
          </button>
        ) : null}
      </div>

      <div className={cn("space-y-3", compact ? "p-3.5" : "p-4")}>
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="truncate text-sm font-medium text-chalk-100">{job.templateTitle}</h3>
            <p className="mt-0.5 text-[11px] text-chalk-400">
              {formatDateTime(job.createdAt)}
              <span className="mx-1.5 text-chalk-500">·</span>
              {formatRelative(job.createdAt)}
            </p>
          </div>
          {job.status === "FAILED" ? (
            <span className="shrink-0 text-[11px] text-coral">已中止</span>
          ) : null}
        </div>

        {pending ? <StageTrack stage={job.stage} progress={job.progress} /> : null}

        {job.status === "FAILED" ? (
          <div className="rounded-lg border border-coral/25 bg-coral/[0.07] px-3 py-2">
            <p className="text-[11px] font-medium text-coral">
              失败于「{STAGE_LABEL[job.errorStage ?? ""] ?? job.errorStage ?? "未知阶段"}」
            </p>
            <p className="mt-0.5 line-clamp-2 text-[11px] text-chalk-300">{job.errorMessage ?? "未知原因"}</p>
          </div>
        ) : null}
      </div>
    </article>
  );
}

function StageTrack({ stage, progress }: { stage: JobStage; progress: number }) {
  const current = STAGE_ORDER[stage] ?? -1;
  return (
    <div className="space-y-2">
      <ol className="flex items-center gap-1.5" aria-label="处理阶段">
        {STEPS.map((step, index) => {
          const done = current > index;
          const active = current === index;
          return (
            <li key={step.stage} className="flex min-w-0 flex-1 items-center gap-1.5">
              <span
                className={cn(
                  "h-1 flex-1 rounded-full transition-colors",
                  done || active ? "bg-mint-500/80" : "bg-white/10",
                  active && "animate-pulse",
                )}
              />
              <span
                className={cn(
                  "shrink-0 text-[10px]",
                  active ? "text-mint-300" : done ? "text-chalk-300" : "text-chalk-500",
                )}
              >
                {step.label}
              </span>
            </li>
          );
        })}
      </ol>
      <div className="flex items-center justify-between text-[10px] text-chalk-400">
        <span>{stage === "QUEUED" ? "排队等待后台处理" : "后台执行中，可继续浏览"}</span>
        <span className="font-mono">{progress}%</span>
      </div>
    </div>
  );
}
