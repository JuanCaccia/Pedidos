"use client";

import { useCallback, useRef, useState } from "react";

interface SwipeButtonProps {
  label: string;
  onConfirm: () => void | Promise<void>;
  disabled?: boolean;
}

const CONFIRM_THRESHOLD = 0.7;

export default function SwipeButton({
  label,
  onConfirm,
  disabled = false,
}: SwipeButtonProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);
  const startX = useRef(0);
  const [progress, setProgress] = useState(0);
  const [draggingState, setDraggingState] = useState(false);
  const [busy, setBusy] = useState(false);

  const trackWidth = () => trackRef.current?.offsetWidth ?? 1;

  const clampProgress = (px: number) => {
    const width = trackWidth();
    const ratio = Math.max(0, Math.min(1, px / width));
    return Math.round(ratio * 100);
  };

  const finishDrag = useCallback(
    async (finalPx: number) => {
      const pct = clampProgress(finalPx);
      if (pct >= CONFIRM_THRESHOLD * 100) {
        setBusy(true);
        try {
          await onConfirm();
        } finally {
          setBusy(false);
        }
      }
      dragging.current = false;
      setDraggingState(false);
      setProgress(0);
    },
    [onConfirm]
  );

  function beginDrag(x: number) {
    if (disabled || busy) return;
    dragging.current = true;
    startX.current = x;
    setDraggingState(true);
  }

  function moveDrag(x: number) {
    if (!dragging.current) return;
    setProgress(clampProgress(x - startX.current));
  }

  function endDrag(x: number) {
    if (!dragging.current) return;
    void finishDrag(x - startX.current);
  }

  function handleKeyDown(event: React.KeyboardEvent) {
    if (event.key !== "Enter" && event.key !== " ") return;
    if (disabled || busy) return;
    event.preventDefault();
    void onConfirm();
  }

  const hint = draggingState ? "soltá para confirmar" : label;
  const fillPct = Math.max(0, Math.min(100, progress));

  return (
    <div className="flex w-full flex-col gap-2">
      <div
        ref={trackRef}
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-label={label}
        aria-disabled={disabled || busy}
        onKeyDown={handleKeyDown}
        onMouseDown={(e) => beginDrag(e.clientX)}
        onMouseMove={(e) => moveDrag(e.clientX)}
        onMouseUp={(e) => endDrag(e.clientX)}
        onMouseLeave={(e) => {
          if (dragging.current) endDrag(e.clientX);
        }}
        onTouchStart={(e) => beginDrag(e.touches[0].clientX)}
        onTouchMove={(e) => moveDrag(e.touches[0].clientX)}
        onTouchEnd={(e) => {
          if (dragging.current) endDrag(e.changedTouches[0].clientX);
        }}
        className={`relative h-16 w-full select-none overflow-hidden rounded-xl border text-center transition-colors ${
          disabled || busy
            ? "cursor-not-allowed border-neutral-300 bg-neutral-100 dark:border-neutral-700 dark:bg-neutral-800"
            : "cursor-grab touch-none border-emerald-600 bg-white active:cursor-grabbing dark:border-emerald-500 dark:bg-neutral-900"
        }`}
      >
        <div
          aria-hidden="true"
          className={`absolute inset-y-0 left-0 rounded-l-xl bg-gradient-to-r from-emerald-500 to-emerald-400 transition-opacity ${
            draggingState ? "" : "opacity-0"
          }`}
          style={{ width: `${fillPct}%` }}
        />
        <span
          className={`relative z-10 flex h-full items-center justify-center px-4 text-sm font-semibold transition-colors ${
            draggingState
              ? "text-emerald-950"
              : disabled || busy
                ? "text-neutral-400"
                : "text-emerald-700 dark:text-emerald-400"
          }`}
        >
          {busy ? "Procesando..." : hint}
        </span>
      </div>
      <div className="flex justify-center">
        <button
          type="button"
          onClick={() => void onConfirm()}
          disabled={disabled || busy}
          className="rounded-md px-3 py-1.5 text-xs font-medium text-neutral-500 underline underline-offset-2 transition-colors hover:text-emerald-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Confirmar entrega
        </button>
      </div>
    </div>
  );
}
