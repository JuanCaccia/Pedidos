"use client";

import { useEffect, useRef } from "react";
import type { ReactNode } from "react";
import { IconClose } from "@/components/icons";

const WIDTH_CLASSES: Record<"md" | "lg", string> = {
  md: "max-w-xl",
  lg: "max-w-2xl",
};

interface DrawerProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  width?: "md" | "lg";
}

export default function Drawer({
  title,
  onClose,
  children,
  width = "lg",
}: DrawerProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const panel = panelRef.current;

    if (panel) panel.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      // If a nested modal is open, focus lives outside the panel and it owns Escape.
      if (panel && panel.contains(document.activeElement)) {
        event.preventDefault();
        onCloseRef.current();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previouslyFocused?.focus();
    };
  }, []);

  return (
    <div className="fixed inset-0 z-40 bg-black/20" onClick={onClose}>
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="false"
        aria-label={title}
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
        className={`fixed inset-y-0 right-0 z-50 w-full overflow-y-auto border-l border-neutral-200 bg-white p-6 outline-none dark:border-neutral-800 dark:bg-neutral-900 ${WIDTH_CLASSES[width]}`}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="rounded-md p-1.5 text-neutral-500 transition-colors hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            <IconClose />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
