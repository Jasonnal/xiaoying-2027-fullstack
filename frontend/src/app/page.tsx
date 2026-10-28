"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { CardSkeleton, EmptyState, ErrorState } from "@/components/StateBlocks";
import { useJobs, useMe, useTemplates } from "@/lib/hooks";
import { countActive } from "@/lib/jobs";
import { cn, formatBytes } from "@/lib/format";
import type { Template } from "@/lib/types";

export default function TemplateListPage() {
  const [category, setCategory] = useState("all");
  const templates = useTemplates(category);
  const me = useMe();
  const jobs = useJobs(Boolean(me.data));

  const categories = useMemo(() => ["all", ...(templates.data?.categories ?? [])], [templates.data?.categories]);
  const active = countActive(jobs.data);

  return (
    <div className="space-y-7">
      <section className="space-y-3">
        <p className="label">Create with templates</p>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h1 className="text-2xl font-semibold tracking-tight text-chalk-100 sm:text-3xl">
            选一个模板，开始这次的制作
          </h1>
          {active > 0 ? (
            <Link
              href="/history"
              className="chip !border-mint-500/30 !bg-mint-500/10 !text-mint-300 hover:border-mint-400"
            >
              {active} 个任务进行中 · 查看进度
            </Link>
          ) : null}
        </div>
        <p className="max-w-2xl text-sm text-chalk-400">
          模板、素材与任务全部由 Java 接口提供。提交后任务在后台按「素材检查 → 模拟生成 → 结果整理」推进，
          期间可以继续浏览或再提交。
        </p>
      </section>

      <div className="flex flex-wrap items-center gap-2" role="group" aria-label="按分类筛选模板">
        {categories.map((item) => {
          const activeTab = category === item;
          return (
            <button
              key={item}
              type="button"
              onClick={() => setCategory(item)}
              aria-pressed={activeTab}
              className={cn(
                "rounded-full border px-3.5 py-1.5 text-xs transition",
                activeTab
                  ? "border-mint-500/50 bg-mint-500/15 text-mint-300"
                  : "border-white/10 bg-white/[0.03] text-chalk-300 hover:border-white/20 hover:text-chalk-100",
              )}
            >
              {item === "all" ? "全部" : item}
            </button>
          );
        })}
      </div>

      {templates.isPending && !templates.data ? <CardSkeleton count={6} /> : null}

      {templates.isError && !templates.data ? (
        <ErrorState
          title="模板加载失败"
          message={templates.error?.message}
          onRetry={() => void templates.refetch()}
        />
      ) : null}

      {templates.data && templates.data.templates.length === 0 ? (
        <EmptyState
          title="这个分类下还没有模板"
          description="换一个分类看看，或者回到全部模板。"
          action={
            <button type="button" className="btn-ghost" onClick={() => setCategory("all")}>
              查看全部模板
            </button>
          }
        />
      ) : null}

      {templates.data && templates.data.templates.length > 0 ? (
        <div
          className={cn(
            "grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3",
            templates.isFetching && "opacity-70 transition-opacity",
          )}
        >
          {templates.data.templates.map((template) => (
            <TemplateCard key={template.id} template={template} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function TemplateCard({ template }: { template: Template }) {
  return (
    <Link
      href={`/templates/${template.id}`}
      className="panel group flex flex-col overflow-hidden transition hover:-translate-y-0.5 hover:border-white/25 hover:shadow-pop"
    >
      <div className="relative aspect-[16/10] overflow-hidden bg-ink-800">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={template.coverUrl}
          alt={`${template.title} 模板封面`}
          className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
          loading="lazy"
        />
        <span className="absolute right-3 top-3 rounded-full bg-ink-950/75 px-2.5 py-1 text-[10px] text-chalk-200 backdrop-blur">
          {template.requiredImageCount} 张素材
        </span>
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-ink-950/95 via-ink-950/40 to-transparent px-4 pb-3 pt-10">
          <p className="text-sm font-semibold text-chalk-100">{template.title}</p>
          <p className="mt-0.5 text-[11px] text-chalk-300">{template.category}</p>
        </div>
      </div>
      <div className="flex items-center justify-between gap-3 px-4 py-3">
        <p className="line-clamp-1 text-xs text-chalk-400">{template.description}</p>
        <span className="shrink-0 text-[10px] text-chalk-500">≤ {formatBytes(template.maxFileSizeBytes)}</span>
      </div>
    </Link>
  );
}
