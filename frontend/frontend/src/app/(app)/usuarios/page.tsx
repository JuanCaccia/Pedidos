"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, apiGet, apiPost } from "@/lib/api";
import type { Usuario, PageResponse } from "@/lib/types";
import ActiveBadge from "@/components/ActiveBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Pagination from "@/components/Pagination";

const ROLES = ["VENDEDOR", "ENCARGADO_DEPOSITO", "REPARTIDOR", "ADMINISTRATIVO"];

const PAGE_SIZE = 20;

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [rolesUsuario, setRolesUsuario] = useState<Usuario | null>(null);
  const [passwordUsuario, setPasswordUsuario] = useState<Usuario | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadUsuarios = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (search.trim()) params.set("q", search.trim());
      const data = await apiGet<PageResponse<Usuario>>(`/api/usuarios?${params.toString()}`);
      setUsuarios(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [search, page]);

  useEffect(() => {
    loadUsuarios();
  }, [loadUsuarios]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      setSearch(searchInput);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  async function toggleActivo(usuario: Usuario) {
    const action = usuario.activo ? "desactivar" : "reactivar";
    try {
      await apiFetch<void>(`/api/usuarios/${usuario.id}/${action}`, { method: "PATCH" });
      await loadUsuarios();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function cambiarPassword(usuario: Usuario, password: string) {
    await apiFetch<void>(`/api/usuarios/${usuario.id}/password`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    setSuccess(`Contraseña de ${usuario.nombre} actualizada.`);
    setPasswordUsuario(null);
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Usuarios</h1>
        <Button
          onClick={() => {
            setSuccess(null);
            setFormError(null);
            setShowForm(true);
          }}
        >
          Nuevo usuario
        </Button>
      </div>

      <input
        type="search"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        placeholder="Buscar por nombre o email..."
        className="w-full max-w-md rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
      />

      {success && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
          {success}
        </div>
      )}

      {error && <ErrorBox message={error} />}

      {loading ? (
        <Loading />
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">Nombre</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 font-medium">Roles</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {usuarios.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-neutral-500">
                      No hay usuarios para mostrar
                    </td>
                  </tr>
                )}
                {usuarios.map((usuario) => (
                  <tr key={usuario.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                    <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">{usuario.nombre}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{usuario.email}</td>
                    <td className="px-4 py-3">
                      <ActiveBadge activo={usuario.activo} />
                    </td>
                    <td className="px-4 py-3">
                      {usuario.roles.length === 0 ? (
                        <span className="text-xs text-neutral-400">Sin roles</span>
                      ) : (
                        <div className="flex flex-wrap gap-1.5">
                          {usuario.roles.map((rol) => (
                            <span
                              key={rol}
                              className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs font-medium text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300"
                            >
                              {rol}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => {
                            setSuccess(null);
                            setRolesUsuario(usuario);
                          }}
                          className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
                        >
                          Roles
                        </button>
                        <button
                          onClick={() => {
                            setSuccess(null);
                            setPasswordUsuario(usuario);
                          }}
                          className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
                        >
                          Cambiar contraseña
                        </button>
                        <button
                          onClick={() => toggleActivo(usuario)}
                          className={`rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-800 ${
                            usuario.activo
                              ? "text-red-600 dark:text-red-400"
                              : "text-emerald-700 dark:text-emerald-400"
                          }`}
                        >
                          {usuario.activo ? "Desactivar" : "Reactivar"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}

      {showForm && (
        <NuevoUsuarioForm
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setShowForm(false)}
          onCreated={async () => {
            setShowForm(false);
            await loadUsuarios();
          }}
        />
      )}

      {rolesUsuario && (
        <RolesPanel
          usuario={rolesUsuario}
          onClose={() => setRolesUsuario(null)}
          onSave={async (roles) => {
            await apiPost<Usuario>(`/api/usuarios/${rolesUsuario.id}/roles`, { roles });
            setRolesUsuario(null);
            await loadUsuarios();
          }}
        />
      )}

      {passwordUsuario && (
        <CambiarPasswordModal
          nombre={passwordUsuario.nombre}
          onClose={() => setPasswordUsuario(null)}
          onSuccess={async (password) => {
            await cambiarPassword(passwordUsuario, password);
          }}
        />
      )}
    </div>
  );
}

function RolesPanel({
  usuario,
  onClose,
  onSave,
}: {
  usuario: Usuario;
  onClose: () => void;
  onSave: (roles: string[]) => Promise<void>;
}) {
  const [selected, setSelected] = useState<string[]>(usuario.roles);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function toggle(rol: string) {
    setSelected((prev) => (prev.includes(rol) ? prev.filter((r) => r !== rol) : [...prev, rol]));
  }

  async function handleSave() {
    if (saving) return;
    setError(null);
    setSaving(true);
    try {
      const union = Array.from(new Set([...usuario.roles, ...selected]));
      await onSave(union);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
      setSaving(false);
    }
  }

  return (
    <Modal title={`Roles de ${usuario.nombre}`} onClose={onClose}>
      <p className="mb-3 text-sm text-neutral-500 dark:text-neutral-400">
        Los roles seleccionados se suman a los actuales.
      </p>

      <div className="flex flex-col gap-2">
        {ROLES.map((rol) => (
          <label
            key={rol}
            className="flex cursor-pointer items-center gap-3 rounded-md border border-neutral-200 px-3 py-2.5 text-sm font-medium text-neutral-800 transition-colors hover:bg-neutral-50 dark:border-neutral-800 dark:text-neutral-200 dark:hover:bg-neutral-800/50"
          >
            <input
              type="checkbox"
              checked={selected.includes(rol)}
              onChange={() => toggle(rol)}
              className="h-4 w-4 rounded border-neutral-300 accent-blue-600"
            />
            {rol}
          </label>
        ))}
      </div>

      {error && <div className="mt-4"><ErrorBox message={error} /></div>}

      <div className="mt-5 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancelar
        </Button>
        <Button type="button" onClick={handleSave} disabled={saving}>
          {saving ? "Guardando..." : "Guardar"}
        </Button>
      </div>
    </Modal>
  );
}

function NuevoUsuarioForm({
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [nombre, setNombre] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [roles, setRoles] = useState<string[]>([]);

  function toggle(rol: string) {
    setRoles((prev) => (prev.includes(rol) ? prev.filter((r) => r !== rol) : [...prev, rol]));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!nombre.trim() || !email.trim() || !password) {
      setError("Completá todos los campos.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/usuarios", {
        nombre: nombre.trim(),
        email: email.trim(),
        password,
        roles,
      });
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Nuevo usuario" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="nombre" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Nombre
            </label>
            <input
              id="nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className={INPUT_CLASS}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="email" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={INPUT_CLASS}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="password" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={INPUT_CLASS}
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Roles</span>
            {ROLES.map((rol) => (
              <label
                key={rol}
                className="flex cursor-pointer items-center gap-3 rounded-md border border-neutral-200 px-3 py-2 text-sm font-medium text-neutral-800 transition-colors hover:bg-neutral-50 dark:border-neutral-800 dark:text-neutral-200 dark:hover:bg-neutral-800/50"
              >
                <input
                  type="checkbox"
                  checked={roles.includes(rol)}
                  onChange={() => toggle(rol)}
                  className="h-4 w-4 rounded border-neutral-300 accent-blue-600"
                />
                {rol}
              </label>
            ))}
          </div>

          {error && <ErrorBox message={error} />}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Creando..." : "Crear usuario"}
            </Button>
          </div>
        </form>
    </Modal>
  );
}

function CambiarPasswordModal({
  nombre,
  onClose,
  onSuccess,
}: {
  nombre: string;
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
    <Modal title={`Cambiar contraseña${nombre ? ` de ${nombre}` : ""}`} onClose={onClose}>
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
