"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { NoticeBar, Spinner } from "./StateBlocks";
import { ApiError } from "@/lib/api";
import { cn, formatBytes } from "@/lib/format";
import { useCreateJob } from "@/lib/hooks";
import { uploadAsset } from "@/lib/upload";
import type { Job, Template, UploadedAsset } from "@/lib/types";

type SlotState =
  | { kind: "empty" }
  | { kind: "uploading"; progress: number; previewUrl: string; name: string }
  | { kind: "ready"; asset: UploadedAsset; previewUrl: string }
  | { kind: "error"; message: string; name: string };

const emptySlots = (count: number): SlotState[] => Array.from({ length: count }, () => ({ kind: "empty" }));

export function UploadPanel({
  template,
  onSubmitted,
}: {
  template: Template;
  onSubmitted?: (job: Job) => void;
}) {
  const [slots, setSlots] = useState<SlotState[]>(() => emptySlots(template.requiredImageCount));
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [justSubmitted, setJustSubmitted] = useState<Job | null>(null);
  const controllers = useRef<Map<number, AbortController>>(new Map());
  const createJob = useCreateJob();

  // 模板切换时重置，并中断还在上传的请求
  useEffect(() => {
    setSlots(emptySlots(template.requiredImageCount));
    setSubmitError(null);
    setJustSubmitted(null);
    const active = controllers.current;
    return () => {
      active.forEach((controller) => controller.abort());
      active.clear();
    };
  }, [template.id, template.requiredImageCount]);

  const patchSlot = useCallback((index: number, next: SlotState) => {
    setSlots((previous) => previous.map((slot, i) => (i === index ? next : slot)));
  }, []);

  const handleFile = useCallback(
    async (index: number, file: File | undefined) => {
      if (!file) {
        return;
      }
      setSubmitError(null);
      setJustSubmitted(null);

      // 客户端先校验一遍，服务端还会再校验一次（数量/类型/大小）
      if (!template.acceptedMimeTypes.includes(file.type)) {
        patchSlot(index, {
          kind: "error",
          name: file.name,
          message: `只支持 ${template.acceptedMimeTypes.map(prettyMime).join(" / ")}，当前是 ${file.type || "未知类型"}`,
        });
        return;
      }
      if (file.size > template.maxFileSizeBytes) {
        patchSlot(index, {
          kind: "error",
          name: file.name,
          message: `文件 ${formatBytes(file.size)} 超过上限 ${formatBytes(template.maxFileSizeBytes)}`,
        });
        return;
      }

      const controller = new AbortController();
      controllers.current.set(index, controller);
      const previewUrl = URL.createObjectURL(file);
      patchSlot(index, { kind: "uploading", progress: 0, previewUrl, name: file.name });

      try {
        const asset = await uploadAsset(file, {
          signal: controller.signal,
          onProgress: (progress) => patchSlot(index, { kind: "uploading", progress, previewUrl, name: file.name }),
        });
        // 上传成功后换成服务端地址：预览与提交都不依赖浏览器临时 URL
        URL.revokeObjectURL(previewUrl);
        patchSlot(index, { kind: "ready", asset, previewUrl: asset.url });
      } catch (error) {
        URL.revokeObjectURL(previewUrl);
        if (error instanceof DOMException && error.name === "AbortError") {
          patchSlot(index, { kind: "empty" });
          return;
        }
        const message = error instanceof ApiError ? error.message : "上传失败，请重试";
        patchSlot(index, { kind: "error", name: file.name, message });
      } finally {
        controllers.current.delete(index);
      }
    },
    [patchSlot, template.acceptedMimeTypes, template.maxFileSizeBytes],
  );

  const removeSlot = (index: number) => {
    controllers.current.get(index)?.abort();
    patchSlot(index, { kind: "empty" });
  };

  const ready = slots.filter((slot): slot is Extract<SlotState, { kind: "ready" }> => slot.kind === "ready");
  const uploading = slots.some((slot) => slot.kind === "uploading");
  const allReady = ready.length === template.requiredImageCount;
  const submitting = createJob.isPending;

  const handleSubmit = () => {
    if (!allReady || submitting) {
      return;
    }
    setSubmitError(null);
    createJob.mutate(
      { templateId: template.id, assetIds: ready.map((slot) => slot.asset.id) },
      {
        onSuccess: (job) => {
          setJustSubmitted(job);
          setSlots(emptySlots(template.requiredImageCount));
          onSubmitted?.(job);
        },
        onError: (error) => setSubmitError(error.message),
      },
    );
  };

  return (
    <section className="panel p-5 sm:p-6" aria-labelledby="upload-title">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 id="upload-title" className="text-base font-semibold text-chalk-100">
            上传素材
          </h2>
          <p className="mt-1 text-xs text-chalk-400">
            需要 {template.requiredImageCount} 张图片 · 支持 {template.acceptedMimeTypes.map(prettyMime).join(" / ")} ·
            单张不超过 {formatBytes(template.maxFileSizeBytes)}
          </p>
        </div>
        <span className="chip">
          {ready.length}/{template.requiredImageCount} 已就绪
        </span>
      </div>

      <div
        className={cn(
          "grid gap-3",
          template.requiredImageCount === 1 ? "grid-cols-1" : "grid-cols-2",
          template.requiredImageCount >= 3 && "sm:grid-cols-3",
        )}
      >
        {slots.map((slot, index) => (
          <Slot
            key={index}
            index={index}
            slot={slot}
            accept={template.acceptedMimeTypes.join(",")}
            onPick={(file) => void handleFile(index, file)}
            onRemove={() => removeSlot(index)}
          />
        ))}
      </div>

      {submitError ? (
        <div className="mt-4">
          <NoticeBar tone="error" title="提交失败" description={submitError} />
        </div>
      ) : null}

      {justSubmitted ? (
        <div className="mt-4">
          <NoticeBar
            tone="success"
            title="任务已受理"
            description={`任务 ${shortId(justSubmitted.id)} 已在后台处理，可继续浏览或提交下一个。`}
          />
        </div>
      ) : null}

      <div className="mt-5 flex flex-wrap items-center gap-3">
        <button
          type="button"
          className="btn-primary min-w-[168px]"
          onClick={handleSubmit}
          disabled={!allReady || submitting}
          aria-busy={submitting}
        >
          {submitting ? <Spinner /> : null}
          {submitting ? "提交中…" : uploading ? "上传中…" : "提交制作任务"}
        </button>
        <p className="text-xs text-chalk-400">
          {allReady
            ? "提交后接口立即返回任务 ID，制作在后台继续，关闭页面也不会中断。"
            : uploading
              ? "图片上传中，请稍候…"
              : `还差 ${Math.max(0, template.requiredImageCount - ready.length)} 张图片`}
        </p>
      </div>
    </section>
  );
}

function Slot({
  index,
  slot,
  accept,
  onPick,
  onRemove,
}: {
  index: number;
  slot: SlotState;
  accept: string;
  onPick: (file: File | undefined) => void;
  onRemove: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const slotLabel = `素材位 ${index + 1}`;

  const openPicker = () => inputRef.current?.click();
  const pick = (files: FileList | null) => {
    onPick(files?.[0]);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  };

  return (
    <div
      className={cn(
        "group relative flex aspect-[4/3] flex-col overflow-hidden rounded-xl border transition",
        slot.kind === "ready"
          ? "border-white/15 bg-ink-800"
          : slot.kind === "error"
            ? "border-coral/40 bg-coral/[0.06]"
            : "border-dashed border-white/15 bg-white/[0.02] hover:border-mint-500/50 hover:bg-mint-500/[0.04]",
        dragging && "border-mint-400 bg-mint-500/10",
      )}
      onDragOver={(event) => {
        event.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDragging(false);
        pick(event.dataTransfer.files);
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="sr-only"
        aria-label={`${slotLabel}：选择图片`}
        onChange={(event) => pick(event.target.files)}
      />

      {slot.kind === "empty" ? (
        <button
          type="button"
          onClick={openPicker}
          className="flex h-full w-full flex-col items-center justify-center gap-2 px-3 text-center"
          aria-label={`${slotLabel}：添加图片`}
        >
          <span className="flex h-9 w-9 items-center justify-center rounded-full border border-white/15 bg-white/[0.04] text-chalk-300">
            <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M12 5v14M5 12h14" strokeLinecap="round" />
            </svg>
          </span>
          <span className="text-xs text-chalk-300">添加图片</span>
          <span className="text-[10px] text-chalk-500">点击或拖拽到此处</span>
        </button>
      ) : null}

      {slot.kind === "uploading" ? (
        <div className="relative h-full w-full">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={slot.previewUrl} alt={`${slotLabel} 正在上传`} className="h-full w-full object-cover opacity-45" />
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-ink-950/40 px-4">
            <Spinner className="h-5 w-5 text-mint-300" />
            <p className="text-[11px] text-chalk-200">上传中 {slot.progress}%</p>
            <div className="h-1 w-3/4 overflow-hidden rounded-full bg-white/15">
              <div className="h-full rounded-full bg-mint-400 transition-all" style={{ width: `${slot.progress}%` }} />
            </div>
          </div>
        </div>
      ) : null}

      {slot.kind === "ready" ? (
        <>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={slot.previewUrl}
            alt={`${slotLabel} 已就绪：${slot.asset.originalName}`}
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-x-0 bottom-0 flex items-center justify-between gap-2 bg-gradient-to-t from-ink-950/90 to-transparent px-2.5 pb-2 pt-6">
            <span className="truncate text-[10px] text-chalk-300">{slot.asset.originalName}</span>
            <span className="flex shrink-0 gap-1">
              <button
                type="button"
                className="rounded-md bg-white/10 px-2 py-1 text-[10px] text-chalk-100 hover:bg-white/20"
                onClick={openPicker}
                aria-label={`${slotLabel}：替换图片`}
              >
                替换
              </button>
              <button
                type="button"
                className="rounded-md bg-white/10 px-2 py-1 text-[10px] text-chalk-100 hover:bg-coral/30"
                onClick={onRemove}
                aria-label={`${slotLabel}：移除图片`}
              >
                移除
              </button>
            </span>
          </div>
          <span className="absolute right-2 top-2 rounded-md bg-mint-500/90 px-1.5 py-0.5 text-[10px] font-medium text-ink-950">
            已就绪
          </span>
        </>
      ) : null}

      {slot.kind === "error" ? (
        <div className="flex h-full flex-col items-center justify-center gap-2 px-3 text-center">
          <p className="text-[11px] font-medium text-coral">上传未完成</p>
          <p className="line-clamp-3 text-[10px] text-chalk-400">{slot.message}</p>
          <div className="flex gap-2">
            <button type="button" className="btn-ghost !px-2 !py-1 !text-[10px]" onClick={openPicker}>
              重新选择
            </button>
            <button type="button" className="btn-ghost !px-2 !py-1 !text-[10px]" onClick={onRemove}>
              清空
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function prettyMime(mime: string): string {
  return mime.replace("image/", "").toUpperCase();
}

function shortId(id: string): string {
  return id.slice(0, 8);
}
