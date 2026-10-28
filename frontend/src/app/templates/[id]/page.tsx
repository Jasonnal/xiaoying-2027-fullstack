"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { JobList } from "@/components/JobList";
import { UploadPanel } from "@/components/UploadPanel";
import { EmptyState, ErrorState } from "@/components/StateBlocks";
import { useJobs, useMe, useTemplate } from "@/lib/hooks";
import { formatBytes } from "@/lib/format";

export default function TemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const templateId = Array.isArray(params?.id) ? params.id[0] : (params?.id ?? "");
  const template = useTemplate(templateId);
  const me = useMe();
  const jobs = useJobs(Boolean(me.data));

  if (template.isPending) {
    return (
      <div className="space-y-5">
        <div className="h-5 w-40 animate-pulse rounded bg-white/10" />
        <div className="grid gap-6 lg:grid-cols-[1.35fr_0.85fr]">
          <div className="space-y-4">
            <div className="panel aspect-[16/9] animate-pulse bg-ink-800" />
            <div className="panel h-64 animate-pulse bg-ink-800/60" />
          </div>
          <div className="panel h-72 animate-pulse bg-ink-800/60" />
        </div>
      </div>
    );
  }

  if (template.isError) {
    const notFound = template.error?.status === 404;
    return (
      <div className="space-y-4">
        <BackLink />
        {notFound ? (
          <EmptyState
            title="模板不存在或已下架"
            description="链接可能已失效，回到列表重新选择即可。"
            action={
              <Link href="/" className="btn-primary">
                返回模板列表
              </Link>
            }
          />
        ) : (
          <ErrorState
            title="模板加载失败"
            message={template.error?.message}
            onRetry={() => void template.refetch()}
          />
        )}
      </div>
    );
  }

  const data = template.data;

  return (
    <div className="space-y-5">
      <BackLink />

      <div className="grid gap-6 lg:grid-cols-[1.35fr_0.85fr] lg:items-start">
        <div className="space-y-5">
          <section className="panel overflow-hidden">
            <div className="relative aspect-[16/9] bg-ink-800">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={data.coverUrl} alt={`${data.title} 示例封面`} className="h-full w-full object-cover" />
              <span className="absolute left-4 top-4 rounded-full bg-ink-950/75 px-3 py-1 text-[11px] text-chalk-200 backdrop-blur">
                {data.category}
              </span>
            </div>
            <div className="space-y-3 p-5 sm:p-6">
              <h1 className="text-xl font-semibold tracking-tight text-chalk-100">{data.title}</h1>
              <p className="text-sm leading-relaxed text-chalk-300">{data.description}</p>
              <dl className="flex flex-wrap gap-x-8 gap-y-2 pt-1 text-xs text-chalk-400">
                <div className="flex items-center gap-1.5">
                  <dt className="text-chalk-500">素材要求</dt>
                  <dd className="text-chalk-200">{data.requiredImageCount} 张</dd>
                </div>
                <div className="flex items-center gap-1.5">
                  <dt className="text-chalk-500">格式</dt>
                  <dd className="text-chalk-200">
                    {data.acceptedMimeTypes.map((mime) => mime.replace("image/", "").toUpperCase()).join(" / ")}
                  </dd>
                </div>
                <div className="flex items-center gap-1.5">
                  <dt className="text-chalk-500">单张上限</dt>
                  <dd className="text-chalk-200">{formatBytes(data.maxFileSizeBytes)}</dd>
                </div>
              </dl>
            </div>
          </section>

          <UploadPanel template={data} />
        </div>

        <div className="lg:sticky lg:top-20">
          <JobList
            jobs={jobs.data}
            isLoading={jobs.isPending}
            isFetching={jobs.isFetching}
            error={jobs.error}
            onRetry={() => void jobs.refetch()}
            compact
            title="我的任务"
            emptyTitle="提交后会出现在这里"
            emptyHint="任务受理即返回，进度由后台推送状态、前端定时刷新。"
          />
        </div>
      </div>
    </div>
  );
}

function BackLink() {
  return (
    <Link
      href="/"
      className="inline-flex items-center gap-1.5 text-xs text-chalk-400 transition hover:text-chalk-100"
    >
      <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M15 6l-6 6 6 6" strokeLinecap="round" />
      </svg>
      模板列表
    </Link>
  );
}
