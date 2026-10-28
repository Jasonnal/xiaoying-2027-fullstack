"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, apiFetch } from "./api";
import { pollingInterval } from "./jobs";
import type { DemoAccount, Job, Me, Template, TemplateListResponse } from "./types";

export const queryKeys = {
  me: ["me"] as const,
  accounts: ["accounts"] as const,
  templates: (category: string) => ["templates", category] as const,
  template: (id: string) => ["template", id] as const,
  jobs: ["jobs"] as const,
};

export function useMe() {
  return useQuery<Me, ApiError>({
    queryKey: queryKeys.me,
    queryFn: ({ signal }) => apiFetch<Me>("/api/me", { signal }),
    retry: false,
    staleTime: 5 * 60 * 1000,
  });
}

export function useDemoAccounts() {
  return useQuery<DemoAccount[], ApiError>({
    queryKey: queryKeys.accounts,
    queryFn: ({ signal }) => apiFetch<DemoAccount[]>("/api/auth/accounts", { signal }),
    staleTime: Infinity,
    retry: 1,
  });
}

export function useTemplates(category: string) {
  const suffix = category === "all" ? "" : `?category=${encodeURIComponent(category)}`;
  return useQuery<TemplateListResponse, ApiError>({
    queryKey: queryKeys.templates(category),
    queryFn: ({ signal }) => apiFetch<TemplateListResponse>(`/api/templates${suffix}`, { signal }),
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
  });
}

export function useTemplate(id: string) {
  return useQuery<Template, ApiError>({
    queryKey: queryKeys.template(id),
    queryFn: ({ signal }) => apiFetch<Template>(`/api/templates/${encodeURIComponent(id)}`, { signal }),
    retry: false,
    staleTime: 60 * 1000,
  });
}

/**
 * 任务列表：唯一的轮询源。
 *
 * - refetchInterval 由数据决定：全部终态后自动停止（没有待处理任务就不再打接口）；
 * - 查询失败时 React Query 保留上一份数据，界面用 error + data 同时判断，
 *   因此"接口挂了"不会被误标成"任务失败"（R-07）。
 */
export function useJobs(enabled: boolean) {
  return useQuery<Job[], ApiError>({
    queryKey: queryKeys.jobs,
    queryFn: ({ signal }) => apiFetch<Job[]>("/api/jobs?limit=50", { signal }),
    enabled,
    refetchInterval: (query) => pollingInterval(query.state.data),
    placeholderData: keepPreviousData,
    retry: 1,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation<Me, ApiError, { handle: string; password: string }>({
    mutationFn: (input) =>
      apiFetch<Me>("/api/auth/session", {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: (me) => {
      // 先落新身份，再把任务缓存整体重置：旧账号的任务/请求结果不会残留（R-08）
      queryClient.setQueryData(queryKeys.me, me);
      void queryClient.resetQueries({ queryKey: queryKeys.jobs });
    },
  });
}

export function useRegister() {
  const queryClient = useQueryClient();
  return useMutation<Me, ApiError, { handle: string; password: string; displayName?: string }>({
    mutationFn: (input) =>
      apiFetch<Me>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: (me) => {
      queryClient.setQueryData(queryKeys.me, me);
      void queryClient.resetQueries({ queryKey: queryKeys.jobs });
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation<void, ApiError, void>({
    mutationFn: () => apiFetch<void>("/api/auth/session", { method: "DELETE" }),
    onSettled: () => {
      // 退出后整页重载：服务端已清 Cookie，重新加载保证不残留任何客户端缓存
      queryClient.clear();
      if (typeof window !== "undefined") {
        window.location.assign("/");
      }
    },
  });
}

export function useCreateJob() {
  const queryClient = useQueryClient();
  return useMutation<Job, ApiError, { templateId: string; assetIds: string[] }>({
    mutationFn: (input) =>
      apiFetch<Job>("/api/jobs", {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: (job) => {
      // 立即把新任务插到列表头部：受理成功后按钮就该恢复，进度由列表轮询刷新
      queryClient.setQueryData<Job[]>(queryKeys.jobs, (previous) => {
        const rest = previous ? previous.filter((item) => item.id !== job.id) : [];
        return [job, ...rest];
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
    },
  });
}
