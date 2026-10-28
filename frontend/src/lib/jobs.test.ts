import { describe, expect, it } from "vitest";
import { countActive, hasActiveJobs, isTerminalStatus, pollingInterval } from "./jobs";
import type { Job, JobStatus } from "./types";

const job = (status: JobStatus): Pick<Job, "status"> => ({ status });

describe("任务轮询策略", () => {
  it("有等待中或处理中的任务时继续刷新", () => {
    expect(pollingInterval([job("PENDING"), job("SUCCEEDED")])).toBe(1500);
    expect(pollingInterval([job("PROCESSING")])).toBe(1500);
    expect(pollingInterval([job("FAILED"), job("PROCESSING")])).toBe(1500);
  });

  it("所有任务到达终态后停止轮询", () => {
    expect(pollingInterval([job("SUCCEEDED"), job("FAILED")])).toBe(false);
    expect(pollingInterval([job("SUCCEEDED")])).toBe(false);
  });

  it("没有任务或数据还没加载时不应发起轮询", () => {
    expect(pollingInterval([])).toBe(false);
    expect(pollingInterval(undefined)).toBe(false);
    expect(pollingInterval(null)).toBe(false);
  });

  it("可以自定义轮询间隔", () => {
    expect(pollingInterval([job("PROCESSING")], 4000)).toBe(4000);
  });
});

describe("状态判定", () => {
  it("统计进行中的任务数", () => {
    expect(countActive([job("PENDING"), job("PROCESSING"), job("FAILED")])).toBe(2);
    expect(countActive(undefined)).toBe(0);
  });

  it("只有成功与失败是终态", () => {
    expect(isTerminalStatus("SUCCEEDED")).toBe(true);
    expect(isTerminalStatus("FAILED")).toBe(true);
    expect(isTerminalStatus("PROCESSING")).toBe(false);
    expect(isTerminalStatus("PENDING")).toBe(false);
  });

  it("hasActiveJobs 对空数据安全", () => {
    expect(hasActiveJobs([])).toBe(false);
    expect(hasActiveJobs(undefined)).toBe(false);
    expect(hasActiveJobs([job("PENDING")])).toBe(true);
  });
});
