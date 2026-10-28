import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { JobList } from "./JobList";
import { ApiError } from "@/lib/api";
import type { Job, JobStatus } from "@/lib/types";

const makeJob = (overrides: Partial<Job> & { id: string; status: JobStatus }): Job => ({
  templateId: "travel-postcard",
  templateTitle: "旅行明信片",
  stage: "QUEUED",
  stageLabel: "排队中",
  progress: 0,
  errorStage: null,
  errorMessage: null,
  resultUrl: null,
  createdAt: "2026-10-28T10:00:00",
  startedAt: null,
  finishedAt: null,
  assets: [{ id: "asset-1", url: "/api/files/uploads/a.png" }],
  ...overrides,
});

describe("JobList", () => {
  it("多个任务的状态、阶段和错误互不串扰", () => {
    const jobs: Job[] = [
      makeJob({
        id: "job-running",
        templateTitle: "旅行明信片",
        status: "PROCESSING",
        stage: "GENERATE",
        stageLabel: "模拟生成",
        progress: 40,
      }),
      makeJob({
        id: "job-failed",
        templateTitle: "复古胶片",
        status: "FAILED",
        stage: "GENERATE",
        errorStage: "GENERATE",
        errorMessage: "模拟生成失败（模拟）：渲染队列返回 500",
      }),
      makeJob({
        id: "job-done",
        templateTitle: "商品主图",
        status: "SUCCEEDED",
        stage: "DONE",
        stageLabel: "已完成",
        progress: 100,
        resultUrl: "/api/files/results/job-done.jpg",
      }),
    ];

    render(<JobList jobs={jobs} isLoading={false} error={null} onRetry={vi.fn()} />);

    const running = screen.getByRole("article", { name: "任务 旅行明信片" });
    expect(within(running).getByText(/处理中/)).toBeInTheDocument();
    expect(within(running).getByText("40%")).toBeInTheDocument();

    const failed = screen.getByRole("article", { name: "任务 复古胶片" });
    expect(within(failed).getByText("失败")).toBeInTheDocument();
    expect(within(failed).getByText(/失败于「模拟生成」/)).toBeInTheDocument();
    expect(within(failed).getByText(/渲染队列返回 500/)).toBeInTheDocument();

    const done = screen.getByRole("article", { name: "任务 商品主图" });
    expect(within(done).getByText("已完成")).toBeInTheDocument();
    expect(within(done).getByRole("button", { name: "查看制作结果" })).toBeInTheDocument();

    // 失败任务的错误信息不能出现在其它卡片里
    expect(within(running).queryByText(/渲染队列返回 500/)).not.toBeInTheDocument();
    expect(within(done).queryByText(/渲染队列返回 500/)).not.toBeInTheDocument();
  });

  it("刷新失败时保留已有任务，只提示请求异常，不把任务标成失败", () => {
    const jobs: Job[] = [
      makeJob({
        id: "job-running",
        templateTitle: "旅行明信片",
        status: "PROCESSING",
        stage: "MATERIAL_CHECK",
        stageLabel: "素材检查",
        progress: 30,
      }),
    ];
    const error = new ApiError(503, "UPSTREAM_UNAVAILABLE", "后端服务暂时不可用，请稍后重试");

    render(<JobList jobs={jobs} isLoading={false} error={error} onRetry={vi.fn()} />);

    expect(screen.getByText("状态刷新失败，任务仍在后台执行")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "立即重试" })).toBeInTheDocument();

    const running = screen.getByRole("article", { name: "任务 旅行明信片" });
    expect(within(running).getByText(/处理中 · 素材检查/)).toBeInTheDocument();
    expect(within(running).queryByText("失败")).not.toBeInTheDocument();
  });

  it("从未拿到数据时展示整块错误与重试入口", () => {
    const error = new ApiError(503, "UPSTREAM_UNAVAILABLE", "后端服务暂时不可用，请稍后重试");
    render(<JobList jobs={undefined} isLoading={false} error={error} onRetry={vi.fn()} />);

    expect(screen.getByText("后端服务暂时不可用，请稍后重试")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "重新加载" })).toBeInTheDocument();
  });

  it("没有任务时给出空状态而不是空白", () => {
    render(<JobList jobs={[]} isLoading={false} error={null} onRetry={vi.fn()} />);
    expect(screen.getByText("还没有制作任务")).toBeInTheDocument();
  });
});
