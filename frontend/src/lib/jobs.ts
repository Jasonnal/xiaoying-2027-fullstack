import type { Job, JobStatus } from "./types";

/** 未结束的状态：需要继续跟踪。 */
export const ACTIVE_STATUSES: JobStatus[] = ["PENDING", "PROCESSING"];

export function isActiveJob(job: Pick<Job, "status">): boolean {
  return ACTIVE_STATUSES.includes(job.status);
}

export function hasActiveJobs(jobs: ReadonlyArray<Pick<Job, "status">> | undefined | null): boolean {
  return Array.isArray(jobs) ? jobs.some(isActiveJob) : false;
}

/**
 * 轮询节奏：有未完成任务才继续问，全部到达终态后返回 false 停止轮询。
 * 单独抽成纯函数，便于单测覆盖"任务结束后停止跟踪"这条要求（R-08）。
 */
export function pollingInterval(
  jobs: ReadonlyArray<Pick<Job, "status">> | undefined | null,
  intervalMs = 1500,
): number | false {
  return hasActiveJobs(jobs) ? intervalMs : false;
}

export function countActive(jobs: ReadonlyArray<Pick<Job, "status">> | undefined | null): number {
  return Array.isArray(jobs) ? jobs.filter(isActiveJob).length : 0;
}

/** 终态判定：完成/失败后再查询也不该回退成处理中。 */
export function isTerminalStatus(status: JobStatus): boolean {
  return status === "SUCCEEDED" || status === "FAILED";
}
