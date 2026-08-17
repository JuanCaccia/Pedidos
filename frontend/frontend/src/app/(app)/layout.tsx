"use client";

import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import type { LoginResponse } from "@/lib/types";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import NotificationsBell from "@/components/NotificationsBell";
import { ToastProvider } from "@/components/Toast";
import { IconMenu } from "@/components/icons";

const NAV_ITEMS = [
  { href: "/", label: "Panel" },
  { href: "/pedidos", label: "Pedidos" },
  { href: "/stock", label: "Stock" },
  { href: "/items", label: "Items" },
  { href: "/clientes", label: "Clientes" },
];

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

export default function AppLayout({ children }: { children: ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const [showPassword, setShowPassword] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  useEffect(() => {
    if (!menuOpen) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setMenuOpen(false);
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [menuOpen]);

  if (loading) {
    return (
      <main className="flex flex-1 items-center justify-center">
        <Loading />
      </main>
    );
  }

  if (!user) return null;

  const usuarioId = user.usuarioId;

  function handleLogout() {
    logout();
    router.replace("/login");
  }

  async function cambiarMiPassword(password: string) {
    await apiFetch<void>(`/api/usuarios/${usuarioId}/password`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    setPasswordSuccess(true);
    setShowPassword(false);
  }

  return (
    <div className="flex min-h-screen bg-neutral-50 dark:bg-neutral-950">
      <aside className="hidden w-56 shrink-0 flex-col border-r border-neutral-200 bg-white md:flex dark:border-neutral-800 dark:bg-neutral-900">
        <SidebarHeader />
        <SidebarNav user={user} pathname={pathname} />
      </aside>

      {menuOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div
            className="fixed inset-0 bg-black/40"
            aria-hidden="true"
            onClick={() => setMenuOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 flex w-56 flex-col border-r border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
            <SidebarHeader />
            <SidebarNav
              user={user}
              pathname={pathname}
              onNavigate={() => setMenuOpen(false)}
            />
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-4 border-b border-neutral-200 bg-white px-6 py-3.5 dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMenuOpen(true)}
              aria-label="Abrir menú"
              className="rounded-md p-2 text-neutral-600 transition-colors hover:bg-neutral-100 md:hidden dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              <IconMenu />
            </button>
            <div className="text-sm text-neutral-500">
              {user.email}
              {user.roles.length > 0 && (
                <span className="ml-2 text-neutral-400">
                  · {user.roles.join(", ")}
                </span>
              )}
            </div>
            <Button
              variant="secondary"
              onClick={() => {
                setPasswordSuccess(false);
                setShowPassword(true);
              }}
            >
              Cambiar mi contraseña
            </Button>
            {passwordSuccess && (
              <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
                Contraseña actualizada
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <NotificationsBell />
            <Button variant="secondary" onClick={handleLogout}>
              Salir
            </Button>
          </div>
        </header>
        <main className="flex-1 px-6 py-6">
          <ToastProvider>{children}</ToastProvider>
        </main>
      </div>

      {showPassword && (
        <CambiarMiPasswordModal
          onClose={() => setShowPassword(false)}
          onSuccess={cambiarMiPassword}
        />
      )}
    </div>
  );
}

function SidebarHeader() {
  return (
    <div className="flex items-center gap-2 border-b border-neutral-200 px-5 py-4 dark:border-neutral-800">
      <span className="text-base font-semibold text-neutral-900 dark:text-neutral-100">Pedidos</span>
    </div>
  );
}

function SidebarNav({
  user,
  pathname,
  onNavigate,
}: {
  user: LoginResponse;
  pathname: string;
  onNavigate?: () => void;
}) {
  function isActive(href: string): boolean {
    if (href === "/") return pathname === "/";
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  function linkClass(href: string): string {
    return `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
      isActive(href)
        ? "bg-blue-50 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300"
        : "text-neutral-600 hover:bg-neutral-100 dark:text-neutral-400 dark:hover:bg-neutral-800"
    }`;
  }

  return (
    <nav className="flex flex-col gap-1 p-3">
      {NAV_ITEMS.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          aria-current={isActive(item.href) ? "page" : undefined}
          className={linkClass(item.href)}
        >
          {item.label}
        </Link>
      ))}
      {(user.roles.includes("ENCARGADO_DEPOSITO") || user.roles.includes("ADMINISTRATIVO")) && (
        <>
          <Link
            href="/proveedores"
            onClick={onNavigate}
            aria-current={isActive("/proveedores") ? "page" : undefined}
            className={linkClass("/proveedores")}
          >
            Proveedores
          </Link>
          <Link
            href="/ordenes-compra"
            onClick={onNavigate}
            aria-current={isActive("/ordenes-compra") ? "page" : undefined}
            className={linkClass("/ordenes-compra")}
          >
            Compras
          </Link>
          <Link
            href="/zonas"
            onClick={onNavigate}
            aria-current={isActive("/zonas") ? "page" : undefined}
            className={linkClass("/zonas")}
          >
            Zonas
          </Link>
        </>
      )}
      {(user.roles.includes("VENDEDOR") || user.roles.includes("ADMINISTRATIVO")) && (
        <Link
          href="/cobranzas"
          onClick={onNavigate}
          aria-current={isActive("/cobranzas") ? "page" : undefined}
          className={linkClass("/cobranzas")}
        >
          Cobranzas
        </Link>
      )}
      {(user.roles.includes("REPARTIDOR") || user.roles.includes("ADMINISTRATIVO")) && (
        <Link
          href="/rutas"
          onClick={onNavigate}
          aria-current={isActive("/rutas") ? "page" : undefined}
          className={linkClass("/rutas")}
        >
          Rutas
        </Link>
      )}
      {(user.roles.includes("REPARTIDOR") || user.roles.includes("ADMINISTRATIVO")) && (
        <Link
          href="/turno"
          onClick={onNavigate}
          aria-current={isActive("/turno") ? "page" : undefined}
          className={linkClass("/turno")}
        >
          Turno
        </Link>
      )}
      {user.roles.includes("ADMINISTRATIVO") && (
        <Link
          href="/usuarios"
          onClick={onNavigate}
          aria-current={isActive("/usuarios") ? "page" : undefined}
          className={linkClass("/usuarios")}
        >
          Usuarios
        </Link>
      )}
    </nav>
  );
}

function CambiarMiPasswordModal({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: (password: string) => Promise<void>;
}) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (saving) return;
    setError(null);

    if (!password) {
      setError("Ingresá la nueva contraseña.");
      return;
    }

    setSaving(true);
    try {
      await onSuccess(password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
      setSaving(false);
    }
  }

  return (
    <Modal title="Cambiar mi contraseña" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Nueva contraseña
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={INPUT_CLASS}
            autoFocus
          />
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Guardando..." : "Guardar"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
