"use client";

import { useEffect, useId, useRef, useState } from "react";
import { IconClose } from "@/components/icons";

export interface ComboboxOption {
  id: number;
  label: string;
  sublabel?: string;
}

interface ComboboxProps {
  value: number | null;
  onChange: (id: number | null) => void;
  search: (query: string) => Promise<ComboboxOption[]>;
  placeholder?: string;
  label?: string;
  required?: boolean;
  disabled?: boolean;
}

const INPUT_CLASS =
  "w-full rounded-md border border-neutral-300 bg-white text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

const DEBOUNCE_MS = 300;

export default function Combobox({
  value,
  onChange,
  search,
  placeholder = "Buscar...",
  label,
  required = false,
  disabled = false,
}: ComboboxProps) {
  const inputId = useId();
  const listboxId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const listboxRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [text, setText] = useState("");
  const [options, setOptions] = useState<ComboboxOption[]>([]);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  const seqRef = useRef(0);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const userTypingRef = useRef(false);

  function runSearch(query: string, seq: number) {
    setLoading(true);
    setError(false);
    search(query)
      .then((result) => {
        if (seq === seqRef.current) {
          setOptions(result);
          setLoading(false);
        }
      })
      .catch(() => {
        if (seq === seqRef.current) {
          setOptions([]);
          setError(true);
          setLoading(false);
        }
      });
  }

  function selectOption(option: ComboboxOption) {
    userTypingRef.current = false;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    seqRef.current += 1;
    setText(option.label);
    setOpen(false);
    setActiveIndex(-1);
    onChange(option.id);
    inputRef.current?.focus();
  }

  function handleClear() {
    userTypingRef.current = false;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    seqRef.current += 1;
    setText("");
    setOptions([]);
    setOpen(false);
    setActiveIndex(-1);
    onChange(null);
    inputRef.current?.focus();
  }

  function handleFocus() {
    if (disabled) return;
    setOpen(true);
    if (options.length === 0 || text === "") {
      runSearch("", ++seqRef.current);
    }
  }

  function handleInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const query = event.target.value;
    userTypingRef.current = true;
    if (value != null) onChange(null);
    setText(query);
    setOpen(true);
    setActiveIndex(-1);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    const seq = ++seqRef.current;
    debounceRef.current = setTimeout(() => runSearch(query, seq), DEBOUNCE_MS);
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (disabled) return;
    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        if (!open) {
          setOpen(true);
          if (options.length === 0) runSearch(text, ++seqRef.current);
          return;
        }
        setActiveIndex((prev) =>
          options.length === 0 ? -1 : Math.min(prev + 1, options.length - 1)
        );
        break;
      case "ArrowUp":
        event.preventDefault();
        if (!open) {
          setOpen(true);
          return;
        }
        setActiveIndex((prev) => Math.max(prev - 1, -1));
        break;
      case "Enter":
        if (open && activeIndex >= 0 && options[activeIndex]) {
          event.preventDefault();
          selectOption(options[activeIndex]);
        } else if (open) {
          event.preventDefault();
          setOpen(false);
        }
        break;
      case "Escape":
        setOpen(false);
        break;
      case "Tab":
        setOpen(false);
        break;
    }
  }

  useEffect(() => {
    function handleMouseDown(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, []);

  useEffect(() => {
    if (value == null) return;
    let cancelled = false;
    const seq = ++seqRef.current;
    search("")
      .then((result) => {
        if (cancelled || seq !== seqRef.current) return;
        const match = result.find((o) => o.id === value);
        if (match) setText(match.label);
      })
      .catch(() => {
        // Sin label resuelto; el usuario puede buscar manualmente
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!open || activeIndex < 0) return;
    const el = listboxRef.current?.querySelector<HTMLElement>(
      `[data-option-index="${activeIndex}"]`
    );
    el?.scrollIntoView({ block: "nearest" });
  }, [open, activeIndex]);

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  const showClear = value != null && !disabled;
  const activeId =
    open && activeIndex >= 0 && options[activeIndex]
      ? `${listboxId}-option-${activeIndex}`
      : undefined;

  return (
    <div ref={rootRef} className="relative flex flex-col gap-1.5">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          id={inputId}
          ref={inputRef}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-haspopup="listbox"
          aria-autocomplete="list"
          aria-controls={open ? listboxId : undefined}
          aria-activedescendant={activeId}
          aria-label={label ?? placeholder}
          aria-required={required}
          disabled={disabled}
          value={text}
          onChange={handleInputChange}
          onFocus={handleFocus}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className={`${INPUT_CLASS} px-3 py-2 ${
            showClear ? "pr-8" : ""
          } ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
        />
        {showClear && (
          <button
            type="button"
            aria-label="Limpiar"
            onClick={handleClear}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-0.5 text-neutral-400 transition-colors hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800"
          >
            <IconClose />
          </button>
        )}
        {open && !disabled && (
          <div
            id={listboxId}
            ref={listboxRef}
            role="listbox"
            className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-md border border-neutral-200 bg-white py-1 shadow-sm dark:border-neutral-700 dark:bg-neutral-900"
          >
            {loading && (
              <div className="px-3 py-2 text-sm text-neutral-500">Buscando...</div>
            )}
            {!loading && error && (
              <div className="px-3 py-2 text-sm text-neutral-500">Error al buscar</div>
            )}
            {!loading && !error && options.length === 0 && (
              <div className="px-3 py-2 text-sm text-neutral-500">Sin resultados</div>
            )}
            {!loading &&
              !error &&
              options.map((option, index) => (
                <div
                  key={option.id}
                  id={`${listboxId}-option-${index}`}
                  role="option"
                  data-option-index={index}
                  aria-selected={value === option.id}
                  onMouseDown={(event) => {
                    event.preventDefault();
                    selectOption(option);
                  }}
                  onMouseEnter={() => setActiveIndex(index)}
                  className={`cursor-pointer px-3 py-2 ${
                    activeIndex === index ? "bg-neutral-100 dark:bg-neutral-800" : ""
                  }`}
                >
                  <div className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">
                    {option.label}
                  </div>
                  {option.sublabel && (
                    <div className="truncate text-xs text-neutral-500 dark:text-neutral-400">
                      {option.sublabel}
                    </div>
                  )}
                </div>
              ))}
          </div>
        )}
      </div>
    </div>
  );
}
