"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, apiGet, apiPost } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { Cliente, Zona, PageResponse } from "@/lib/types";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Pagination from "@/components/Pagination";
import { exportarCSV } from "@/lib/export";

const PAGE_SIZE = 20;

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

interface ClienteFormValues {
  razonSocial: string;
  cuit: string;
  email: string | null;
  telefono: string | null;
  domicilio: string | null;
  zonaId: number;
}

export default function ClientesPage() {
  const { user } = useAuth();
  const canGestionar = (user?.roles.includes("VENDEDOR") || user?.roles.includes("ADMINISTRATIVO")) ?? false;
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [zonas, setZonas] = useState<Zona[]>([]);
  const [zonaId, setZonaId] = useState<string>("");
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [reloadKey, setReloadKey] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);

  const loadClientes = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (search.trim()) params.set("q", search.trim());
      if (zonaId) params.set("zonaId", zonaId);
      const data = await apiGet<PageResponse<Cliente>>(`/api/clientes?${params.toString()}`);
      setClientes(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [search, zonaId, page, reloadKey]);

  useEffect(() => {
    loadClientes();
  }, [loadClientes]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      setSearch(searchInput);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    apiGet<Zona[]>("/api/zonas")
      .then(setZonas)
      .catch(() => {
        // El select de zona quedará vacío si no se pueden cargar las zonas
      });
  }, []);

  const editCliente = clientes.find((c) => c.id === editId) ?? null;

  async function exportar() {
    try {
      await exportarCSV(
        "/api/clientes/exportar.csv" + (zonaId ? "?zonaId=" + zonaId : ""),
        "clientes.csv"
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function crearCliente(values: ClienteFormValues) {
    await apiPost("/api/clientes", {
      razonSocial: values.razonSocial,
      cuit: values.cuit,
      email: values.email,
      telefono: values.telefono,
      domicilio: values.domicilio,
      zonaId: values.zonaId,
    });
    setSuccess("Cliente creado correctamente.");
    setShowForm(false);
    setPage(0);
    setReloadKey((k) => k + 1);
  }

  async function actualizarCliente(values: ClienteFormValues) {
    if (editId === null) return;
    await apiFetch(`/api/clientes/${editId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        razonSocial: values.razonSocial,
        email: values.email,
        telefono: values.telefono,
        domicilio: values.domicilio,
        zonaId: values.zonaId,
      }),
    });
    setSuccess("Cliente actualizado correctamente.");
    setEditId(null);
    setReloadKey((k) => k + 1);
  }

  async function toggleActivo(cliente: Cliente) {
    const action = cliente.activo ? "desactivar" : "reactivar";
    try {
      await apiFetch<void>(`/api/clientes/${cliente.id}/${action}`, { method: "PATCH" });
      setSuccess(cliente.activo ? "Cliente desactivado." : "Cliente reactivado.");
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Clientes</h1>
        {canGestionar && (
          <Button
            onClick={() => {
              setSuccess(null);
              setShowForm(true);
            }}
          >
            Nuevo cliente
          </Button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Buscar por razón social o CUIT..."
          className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
        />
        <label htmlFor="zona" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
          Zona
        </label>
        <select
          id="zona"
          value={zonaId}
          onChange={(e) => {
            setPage(0);
            setZonaId(e.target.value);
          }}
          className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
        >
          <option value="">Todas</option>
          {zonas.map((z) => (
            <option key={z.id} value={z.id}>
              {z.nombre}
            </option>
          ))}
        </select>
        <Button variant="secondary" className="px-3 py-1.5" onClick={exportar}>
          Exportar CSV
        </Button>
      </div>

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
                  <th className="px-4 py-3 font-medium">Razón social</th>
                  <th className="px-4 py-3 font-medium">CUIT</th>
                  <th className="px-4 py-3 font-medium">Zona</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Teléfono</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {clientes.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay clientes para mostrar
                    </td>
                  </tr>
                )}
                {clientes.map((cliente) => (
                  <tr key={cliente.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                    <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">
                      {cliente.razonSocial}
                      {!cliente.activo && (
                        <span className="ml-2 rounded-full bg-neutral-200 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-700 dark:text-neutral-300">
                          inactivo
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono text-neutral-700 dark:text-neutral-300">{cliente.cuit}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cliente.zonaNombre ?? "-"}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cliente.email ?? "-"}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cliente.telefono ?? "-"}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        {canGestionar && (
                          <Button
                            variant="secondary"
                            onClick={() => {
                              setSuccess(null);
                              setError(null);
                              setEditId(cliente.id);
                            }}
                          >
                            Editar
                          </Button>
                        )}
                        {canGestionar && cliente.activo && (
                          <button
                            onClick={() => toggleActivo(cliente)}
                            className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 dark:border-neutral-700 dark:text-red-400 dark:hover:bg-red-950/40"
                          >
                            Desactivar
                          </button>
                        )}
                        {canGestionar && !cliente.activo && (
                          <button
                            onClick={() => toggleActivo(cliente)}
                            className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-emerald-700 transition-colors hover:bg-emerald-50 dark:border-neutral-700 dark:text-emerald-400 dark:hover:bg-emerald-950/40"
                          >
                            Reactivar
                          </button>
                        )}
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
        <ClienteFormModal
          title="Nuevo cliente"
          cliente={null}
          zonas={zonas}
          onClose={() => setShowForm(false)}
          onSubmit={crearCliente}
        />
      )}

      {editCliente && (
        <ClienteFormModal
          title={`Editar cliente ${editCliente.razonSocial}`}
          cliente={editCliente}
          zonas={zonas}
          onClose={() => setEditId(null)}
          onSubmit={actualizarCliente}
        />
      )}
    </div>
  );
}

function ClienteFormModal({
  title,
  cliente,
  zonas,
  onClose,
  onSubmit,
}: {
  title: string;
  cliente: Cliente | null;
  zonas: Zona[];
  onClose: () => void;
  onSubmit: (values: ClienteFormValues) => Promise<void>;
}) {
  const isEdit = cliente !== null;
  const [razonSocial, setRazonSocial] = useState(cliente?.razonSocial ?? "");
  const [cuit, setCuit] = useState(cliente?.cuit ?? "");
  const [email, setEmail] = useState(cliente?.email ?? "");
  const [telefono, setTelefono] = useState(cliente?.telefono ?? "");
  const [domicilio, setDomicilio] = useState(cliente?.domicilio ?? "");
  const [zona, setZona] = useState(cliente?.zonaId != null ? String(cliente.zonaId) : "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (saving) return;
    setError(null);

    if (!razonSocial.trim()) {
      setError("Ingresá la razón social.");
      return;
    }
    if (!isEdit && !/^\d{11}$/.test(cuit.trim())) {
      setError("El CUIT debe tener exactamente 11 dígitos.");
      return;
    }
    if (!zona) {
      setError("Seleccioná una zona.");
      return;
    }

    setSaving(true);
    try {
      await onSubmit({
        razonSocial: razonSocial.trim(),
        cuit: cuit.trim(),
        email: email.trim() === "" ? null : email.trim(),
        telefono: telefono.trim() === "" ? null : telefono.trim(),
        domicilio: domicilio.trim() === "" ? null : domicilio.trim(),
        zonaId: Number(zona),
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose} width="lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="cliente-razonSocial" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Razón social
          </label>
          <input
            id="cliente-razonSocial"
            value={razonSocial}
            onChange={(e) => setRazonSocial(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        {isEdit ? (
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">CUIT</span>
            <div className="rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2 font-mono text-sm text-neutral-500 dark:border-neutral-800 dark:bg-neutral-800/50 dark:text-neutral-400">
              {cliente.cuit}
            </div>
            <p className="text-xs text-neutral-500 dark:text-neutral-400">El CUIT no se puede modificar.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cliente-cuit" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              CUIT
            </label>
            <input
              id="cliente-cuit"
              value={cuit}
              onChange={(e) => setCuit(e.target.value)}
              inputMode="numeric"
              maxLength={11}
              pattern="[0-9]{11}"
              className={INPUT_CLASS}
              placeholder="Ej.: 20123456789"
            />
            <p className="text-xs text-neutral-500 dark:text-neutral-400">Debe tener exactamente 11 dígitos.</p>
          </div>
        )}

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cliente-email" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Email
          </label>
          <input
            id="cliente-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cliente-telefono" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Teléfono
          </label>
          <input
            id="cliente-telefono"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cliente-domicilio" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Domicilio
          </label>
          <input
            id="cliente-domicilio"
            value={domicilio}
            onChange={(e) => setDomicilio(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cliente-zona" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Zona
          </label>
          <select
            id="cliente-zona"
            value={zona}
            onChange={(e) => setZona(e.target.value)}
            className={INPUT_CLASS}
          >
            <option value="">Seleccionar zona</option>
            {zonas.map((z) => (
              <option key={z.id} value={z.id}>
                {z.nombre}
              </option>
            ))}
          </select>
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Guardando..." : isEdit ? "Guardar cambios" : "Crear cliente"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
