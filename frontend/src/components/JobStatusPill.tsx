import { cn } from "@/lib/format";
import type { JobStatus } from "@/lib/types";

const STYLES: Record<JobStatus, { label: string; className: string; dot: string }> = {
  PENDING: { label: "等待处理", className: "border-ember/30 bg-ember/10 text-ember", dot: "bg-ember" },
  PROCESSING: {
    label: "处理中",
    className: "border-mint-500/30 bg-mint-500/10 text-mint-300",
    dot: "bg-mint-400",
  },
  SUCCEEDED: {
    label: "已完成",
    className: "border-mint-500/25 bg-mint-500/10 text-mint-300",
    dot: "bg-mint-500",
  },
  FAILED: { label: "失败", className: "border-coral/30 bg-coral/10 text-coral", dot: "bg-coral" },
};

export function JobStatusPill({
  status,
  stageLabel,
  compact = false,
}: {
  status: JobStatus;
  stageLabel?: string;
  compact?: boolean;
}) {
  const style = STYLES[status];
  const text =
    status === "PROCESSING" && stageLabel && !compact ? `处理中 · ${stageLabel}` : style.label;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-medium",
        style.className,
      )}
    >
      <span className="relative flex h-1.5 w-1.5">
        <span className={cn("h-1.5 w-1.5 rounded-full", style.dot)} />
        {status === "PROCESSING" ? (
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint-400 opacity-70" />
        ) : null}
      </span>
      {text}
    </span>
  );
}
