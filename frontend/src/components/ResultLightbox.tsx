"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

export function ResultLightbox({
  url,
  title,
  onClose,
}: {
  url: string;
  title: string;
  onClose: () => void;
}) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  if (!mounted) {
    return null;
  }

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-ink-950/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label={`${title} 制作结果`}
      onClick={onClose}
    >
      <div
        className="panel w-full max-w-4xl overflow-hidden animate-rise"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between gap-3 border-b border-white/10 px-5 py-3">
          <div>
            <p className="text-sm font-medium text-chalk-100">{title}</p>
            <p className="text-[11px] text-chalk-400">制作结果预览</p>
          </div>
          <div className="flex items-center gap-2">
            <a className="btn-ghost !py-1.5 !text-xs" href={url} target="_blank" rel="noreferrer">
              新窗口打开
            </a>
            <button type="button" className="btn-ghost !py-1.5 !text-xs" onClick={onClose} autoFocus>
              关闭
            </button>
          </div>
        </div>
        <div className="bg-ink-900 p-3">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={url} alt={`${title} 结果图`} className="mx-auto max-h-[74vh] w-auto rounded-lg" />
        </div>
      </div>
    </div>,
    document.body,
  );
}
