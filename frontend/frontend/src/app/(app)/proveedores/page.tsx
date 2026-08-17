"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch, apiGet, apiPost } from "@/lib/api";
import type { Item, PageResponse, Proveedor, ProveedorItem } from "@/lib/types";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Pagination from "@/components/Pagination";
import Combobox from "@/components/Combobox";
import { IconClose } from "@/components/icons";

const PAGE_SIZE = 20;

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

interface ProveedorFormValues {
  razonSocial: string;
  cuit: string;
  email: string | null;
  telefono: string | null;
}

export default function ProveedoresPage() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [reloadKey, setReloadKey] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [itemsProveedor, setItemsProveedor] = useState<Proveedor | null>(null);

  const loadProveedores = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (search.trim()) params.set("q", search.trim());
      const data = await apiGet<PageResponse<Proveedor>>(`/api/proveedores?${params.toString()}`);
      setProveedores(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [search, page, reloadKey]);

  useEffect(() => {
    loadProveedores();
  }, [loadProveedores]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      setSearch(searchInput);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const editProveedor = proveedores.find((p) => p.id === editId) ?? null;

  async function crearProveedor(values: ProveedorFormValues) {
    await apiPost("/api/proveedores", {
      razonSocial: values.razonSocial,
      cuit: values.cuit,
      email: values.email,
      telefono: values.telefono,
    });
    setSuccess("Proveedor creado correctamente.");
    setShowForm(false);
    setReloadKey((k) => k + 1);
  }

  async function actualizarProveedor(values: ProveedorFormValues) {
    if (editId === null) return;
    await apiFetch(`/api/proveedores/${editId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        razonSocial: values.razonSocial,
        email: values.email,
        telefono: values.telefono,
      }),
    });
    setSuccess("Proveedor actualizado correctamente.");
    setEditId(null);
    setReloadKey((k) => k + 1);
  }

  async function toggleActivo(proveedor: Proveedor) {
    const action = proveedor.activo ? "desactivar" : "reactivar";
    try {
      await apiFetch<void>(`/api/proveedores/${proveedor.id}/${action}`, { method: "PATCH" });
      setSuccess(proveedor.activo ? "Proveedor desactivado." : "Proveedor reactivado.");
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Proveedores</h1>
        <Button
          onClick={() => {
            setSuccess(null);
            setError(null);
            setShowForm(true);
          }}
        >
          Nuevo proveedor
        </Button>
      </div>

      <input
        type="search"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        placeholder="Buscar por razón social o CUIT..."
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
                  <th className="px-4 py-3 font-medium">Razón social</th>
                  <th className="px-4 py-3 font-medium">CUIT</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Teléfono</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {proveedores.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay proveedores para mostrar
                    </td>
                  </tr>
                )}
                {proveedores.map((proveedor) => (
                  <tr key={proveedor.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                    <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">
                      {proveedor.razonSocial}
                    </td>
                    <td className="px-4 py-3 font-mono text-neutral-700 dark:text-neutral-300">{proveedor.cuit}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{proveedor.email ?? "-"}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{proveedor.telefono ?? "-"}</td>
                    <td className="px-4 py-3">
                      {proveedor.activo ? (
                        <span className="inline-flex rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">
                          Activo
                        </span>
                      ) : (
                        <span className="inline-flex rounded-full bg-neutral-200 px-2.5 py-0.5 text-xs font-medium text-neutral-600 dark:bg-neutral-700 dark:text-neutral-300">
                          Inactivo
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setSuccess(null);
                            setError(null);
                            setItemsProveedor(proveedor);
                          }}
                        >
                          Items
                        </Button>
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setSuccess(null);
                            setError(null);
                            setEditId(proveedor.id);
                          }}
                        >
                          Editar
                        </Button>
                        {proveedor.activo && (
                          <button
                            onClick={() => toggleActivo(proveedor)}
                            className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 dark:border-neutral-700 dark:text-red-400 dark:hover:bg-red-950/40"
                          >
                            Desactivar
                          </button>
                        )}
                        {!proveedor.activo && (
                          <button
                            onClick={() => toggleActivo(proveedor)}
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
        <ProveedorFormModal
          title="Nuevo proveedor"
          proveedor={null}
          onClose={() => setShowForm(false)}
          onSubmit={crearProveedor}
        />
      )}

      {editProveedor && (
        <ProveedorFormModal
          title={`Editar proveedor ${editProveedor.razonSocial}`}
          proveedor={editProveedor}
          onClose={() => setEditId(null)}
          onSubmit={actualizarProveedor}
        />
      )}

      {itemsProveedor && (
        <ProveedorItemsModal proveedor={itemsProveedor} onClose={() => setItemsProveedor(null)} />
      )}
    </div>
  );
}

function ProveedorFormModal({
  title,
  proveedor,
  onClose,
  onSubmit,
}: {
  title: string;
  proveedor: Proveedor | null;
  onClose: () => void;
  onSubmit: (values: ProveedorFormValues) => Promise<void>;
}) {
  const isEdit = proveedor !== null;
  const [razonSocial, setRazonSocial] = useState(proveedor?.razonSocial ?? "");
  const [cuit, setCuit] = useState(proveedor?.cuit ?? "");
  const [email, setEmail] = useState(proveedor?.email ?? "");
  const [telefono, setTelefono] = useState(proveedor?.telefono ?? "");
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

    setSaving(true);
    try {
      await onSubmit({
        razonSocial: razonSocial.trim(),
        cuit: cuit.trim(),
        email: email.trim() === "" ? null : email.trim(),
        telefono: telefono.trim() === "" ? null : telefono.trim(),
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
          <label htmlFor="proveedor-razonSocial" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Razón social
          </label>
          <input
            id="proveedor-razonSocial"
            value={razonSocial}
            onChange={(e) => setRazonSocial(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        {isEdit ? (
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">CUIT</span>
            <div className="rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2 font-mono text-sm text-neutral-500 dark:border-neutral-800 dark:bg-neutral-800/50 dark:text-neutral-400">
              {proveedor.cuit}
            </div>
            <p className="text-xs text-neutral-500 dark:text-neutral-400">El CUIT no se puede modificar.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-1.5">
            <label htmlFor="proveedor-cuit" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              CUIT
            </label>
            <input
              id="proveedor-cuit"
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
          <label htmlFor="proveedor-email" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Email
          </label>
          <input
            id="proveedor-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="proveedor-telefono" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Teléfono
          </label>
          <input
            id="proveedor-telefono"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Guardando..." : isEdit ? "Guardar cambios" : "Crear proveedor"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function ProveedorItemsModal({
  proveedor,
  onClose,
}: {
  proveedor: Proveedor;
  onClose: () => void;
}) {
  const [items, setItems] = useState<ProveedorItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [pickerKey, setPickerKey] = useState(0);

  const loadItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiGet<ProveedorItem[]>(`/api/proveedores/${proveedor.id}/items`);
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [proveedor.id]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  const seleccionados = useMemo(() => new Set(items.map((i) => i.itemId)), [items]);

  async function agregarItem(itemId: number) {
    if (seleccionados.has(itemId)) return;
    setItems((prev) => [
      ...prev,
      { proveedorId: proveedor.id, itemId, itemSku: null, itemNombre: null, activo: true },
    ]);
    setPickerKey((k) => k + 1);
  }

  function quitarItem(itemId: number) {
    setItems((prev) => prev.filter((i) => i.itemId !== itemId));
  }

  async function handleSave() {
    if (saving) return;
    setError(null);
    setSaving(true);
    try {
      const saved = await apiFetch<ProveedorItem[]>(`/api/proveedores/${proveedor.id}/items`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ itemIds: items.map((i) => i.itemId) }),
      });
      setItems(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Catálogo de ${proveedor.razonSocial}`} onClose={onClose} width="lg">
      <div className="flex flex-col gap-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          Definí qué items provee este proveedor. Al crear una orden de compra solo se
          admiten items del catálogo activo.
        </p>

        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Agregar item</span>
          <Combobox
            key={pickerKey}
            placeholder="Buscar item para vincular..."
            value={null}
            onChange={(id) => {
              if (id != null) agregarItem(id);
            }}
            search={async (q) => {
              const params = new URLSearchParams({ q, size: "20", activos: "true" });
              const data = await apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`);
              return data.content.map((i) => ({
                id: i.id,
                label: `${i.sku} — ${i.nombre}`,
                sublabel: i.categoriaNombre ?? "",
              }));
            }}
          />
        </div>

        {loading ? (
          <Loading />
        ) : (
          <div className="flex flex-col gap-2">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Items en el catálogo ({items.length})
            </span>
            {items.length === 0 && (
              <p className="text-sm text-neutral-500">No hay items vinculados a este proveedor.</p>
            )}
            <div className="flex flex-col gap-2">
              {items.map((item) => (
                <div
                  key={item.itemId}
                  className="flex items-center justify-between gap-3 rounded-md border border-neutral-200 px-3 py-2 dark:border-neutral-700"
                >
                  <div className="min-w-0">
                    <span className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">
                      {item.itemNombre ?? `#${item.itemId}`}
                    </span>
                    {item.itemSku && (
                      <span className="ml-2 font-mono text-xs text-neutral-500 dark:text-neutral-400">
                        {item.itemSku}
                      </span>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => quitarItem(item.itemId)}
                    aria-label={`Quitar item ${item.itemId}`}
                    className="rounded-md border border-neutral-200 p-1.5 text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-red-600 dark:border-neutral-700 dark:hover:bg-neutral-800 dark:hover:text-red-400"
                  >
                    <IconClose />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="button" onClick={handleSave} disabled={saving || loading}>
            {saving ? "Guardando..." : "Guardar catálogo"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
