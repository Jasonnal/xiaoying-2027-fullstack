"use client";

import { JobList } from "@/components/JobList";
import { Spinner } from "@/components/StateBlocks";
import { useJobs, useMe } from "@/lib/hooks";
import { countActive } from "@/lib/jobs";
import type { JobStatus } from "@/lib/types";

export default function HistoryPage() {
  const me = useMe();
  const jobs = useJobs(Boolean(me.data));

  const stats: Array<{ label: string; value: number; tone: JobStatus | "ALL" }> = [
    { label: "全部", value: jobs.data?.length ?? 0, tone: "ALL" },
    { label: "进行中", value: countActive(jobs.data), tone: "PROCESSING" },
    { label: "已完成", value: jobs.data?.filter((job) => job.status === "SUCCEEDED").length ?? 0, tone: "SUCCEEDED" },
    { label: "失败", value: jobs.data?.filter((job) => job.status === "FAILED").length ?? 0, tone: "FAILED" },
  ];

  return (
    <div className="space-y-6">
      <section className="flex flex-wrap items-end justify-between gap-3">
        <div className="space-y-2">
          <p className="label">History</p>
          <h1 className="text-2xl font-semibold tracking-tight text-chalk-100 sm:text-3xl">历史记录</h1>
          <p className="text-sm text-chalk-400">
            记录按创建时间倒序，来自 Java 后端；刷新页面会重新拉取，未结束的任务会继续被跟踪。
          </p>
        </div>
        <button
          type="button"
          className="btn-ghost"
          onClick={() => void jobs.refetch()}
          disabled={jobs.isFetching}
        >
          {jobs.isFetching ? <Spinner /> : null}
          刷新状态
        </button>
      </section>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {stats.map((item) => (
          <div key={item.label} className="panel px-4 py-3">
            <p className="text-[11px] text-chalk-400">{item.label}</p>
            <p className="mt-1 font-mono text-xl text-chalk-100">{item.value}</p>
          </div>
        ))}
      </div>

      <JobList
        jobs={jobs.data}
        isLoading={jobs.isPending}
        isFetching={jobs.isFetching}
        error={jobs.error}
        onRetry={() => void jobs.refetch()}
        title="全部任务"
        emptyTitle="还没有任何制作任务"
        emptyHint="去模板列表挑一个，上传素材后提交，就能在这里看到进度与结果。"
      />
    </div>
  );
}
