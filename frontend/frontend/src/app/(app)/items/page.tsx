"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, apiGet, apiPost } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { Item, PageResponse } from "@/lib/types";
import { formatMoney, formatNumber } from "@/lib/format";
import ActiveBadge from "@/components/ActiveBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Pagination from "@/components/Pagination";

const PAGE_SIZE = 20;

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

export default function ItemsPage() {
  const { user } = useAuth();
  const canGestionar =
    (user?.roles.includes("ENCARGADO_DEPOSITO") || user?.roles.includes("ADMINISTRATIVO")) ?? false;
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [categoriaFilter, setCategoriaFilter] = useState("");
  const [categorias, setCategorias] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [editId, setEditId] = useState<number | null>(null);

  const loadItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (search.trim()) params.set("q", search.trim());
      if (categoriaFilter) params.set("categoria", categoriaFilter);
      const data = await apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`);
      setItems(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [search, categoriaFilter, page]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  useEffect(() => {
    apiGet<string[]>("/api/items/categorias")
      .then(setCategorias)
      .catch(() => {
        // El filtro de categorías mostrará solo "Todas las categorías"
      });
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      setSearch(searchInput);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const editItem = items.find((i) => i.id === editId) ?? null;

  async function guardarItem(
    id: number,
    nombre: string,
    unidadMedida: string,
    stockMinimo: number | undefined,
    precioLista: number | undefined,
    categoria: string
  ) {
    const body: Record<string, unknown> = { nombre, unidadMedida };
    if (stockMinimo !== undefined) body.stockMinimo = stockMinimo;
    if (precioLista !== undefined) body.precioLista = precioLista;
    if (categoria.trim()) body.categoria = categoria.trim();
    await apiFetch<Item>(`/api/items/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    setEditId(null);
    await loadItems();
  }

  async function desactivar(item: Item) {
    try {
      await apiFetch<void>(`/api/items/${item.id}/desactivar`, { method: "PATCH" });
      await loadItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function reactivar(item: Item) {
    try {
      await apiFetch<void>(`/api/items/${item.id}/reactivar`, { method: "PATCH" });
      await loadItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Items</h1>
        {canGestionar && (
          <Button
            onClick={() => {
              setFormError(null);
              setShowForm(true);
            }}
          >
            Nuevo item
          </Button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Buscar por nombre o SKU..."
          className="w-full max-w-md rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
        />
        <select
          value={categoriaFilter}
          onChange={(e) => {
            setPage(0);
            setCategoriaFilter(e.target.value);
          }}
          className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
        >
          <option value="">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {error && <ErrorBox message={error} />}

      {loading ? (
        <Loading />
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">SKU</th>
                  <th className="px-4 py-3 font-medium">Nombre</th>
                  <th className="px-4 py-3 font-medium">Categoría</th>
                  <th className="px-4 py-3 font-medium">Unidad de medida</th>
                  <th className="px-4 py-3 font-medium">Stock mín.</th>
                  <th className="px-4 py-3 font-medium">Precio lista</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {items.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-neutral-500">
                      No hay items para mostrar
                    </td>
                  </tr>
                )}
                {items.map((item) => (
                  <tr key={item.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                    <td className="px-4 py-3 font-mono text-neutral-700 dark:text-neutral-300">{item.sku}</td>
                    <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">{item.nombre}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{item.categoria ?? "—"}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{item.unidadMedida}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{formatNumber(item.stockMinimo)}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{formatMoney(item.precioLista)}</td>
                    <td className="px-4 py-3">
                      <ActiveBadge activo={item.activo} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        {canGestionar && (
                          <Button
                            variant="secondary"
                            onClick={() => {
                              setError(null);
                              setEditId(item.id);
                            }}
                          >
                            Editar
                          </Button>
                        )}
                        {canGestionar && item.activo && (
                          <button
                            onClick={() => desactivar(item)}
                            className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 dark:border-neutral-700 dark:text-red-400 dark:hover:bg-red-950/40"
                          >
                            Desactivar
                          </button>
                        )}
                        {canGestionar && !item.activo && (
                          <button
                            onClick={() => reactivar(item)}
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
        <NuevoItemForm
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setShowForm(false)}
          onCreated={async () => {
            setShowForm(false);
            await loadItems();
          }}
        />
      )}

      {editItem && (
        <EditarItemModal
          item={editItem}
          onSave={async (nombre, unidadMedida, stockMinimo, precioLista, categoria) => {
            await guardarItem(editItem.id, nombre, unidadMedida, stockMinimo, precioLista, categoria);
          }}
          onClose={() => setEditId(null)}
        />
      )}
    </div>
  );
}

function EditarItemModal({
  item,
  onSave,
  onClose,
}: {
  item: Item;
  onSave: (
    nombre: string,
    unidadMedida: string,
    stockMinimo: number | undefined,
    precioLista: number | undefined,
    categoria: string
  ) => Promise<void>;
  onClose: () => void;
}) {
  const [nombre, setNombre] = useState(item.nombre);
  const [unidadMedida, setUnidadMedida] = useState(item.unidadMedida);
  const [stockMinimo, setStockMinimo] = useState(String(item.stockMinimo));
  const [precioLista, setPrecioLista] = useState(String(item.precioLista));
  const [categoria, setCategoria] = useState(item.categoria ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (saving) return;
    setError(null);
    if (!nombre.trim() || !unidadMedida.trim()) {
      setError("Completá todos los campos.");
      return;
    }
    let stockMinimoValue: number | undefined;
    if (stockMinimo.trim() !== "") {
      stockMinimoValue = Number(stockMinimo);
      if (Number.isNaN(stockMinimoValue) || stockMinimoValue < 0) {
        setError("El stock mínimo debe ser un número mayor o igual a cero.");
        return;
      }
    }
    let precioListaValue: number | undefined;
    if (precioLista.trim() !== "") {
      precioListaValue = Number(precioLista);
      if (Number.isNaN(precioListaValue) || precioListaValue < 0) {
        setError("El precio de lista debe ser un número mayor o igual a cero.");
        return;
      }
    }
    setSaving(true);
    try {
      await onSave(nombre.trim(), unidadMedida.trim(), stockMinimoValue, precioListaValue, categoria.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Editar item ${item.sku}`} onClose={onClose}>
      <form onSubmit={handleSave} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-nombre" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Nombre
          </label>
          <input
            id="edit-nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-unidad" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Unidad de medida
          </label>
          <input
            id="edit-unidad"
            value={unidadMedida}
            onChange={(e) => setUnidadMedida(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-categoria" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Categoría
          </label>
          <input
            id="edit-categoria"
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Ej.: Harinas"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-stockMinimo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Stock mínimo
          </label>
          <input
            id="edit-stockMinimo"
            type="number"
            min="0"
            step="0.001"
            value={stockMinimo}
            onChange={(e) => setStockMinimo(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-precioLista" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Precio lista
          </label>
          <input
            id="edit-precioLista"
            type="number"
            min="0"
            step="0.01"
            value={precioLista}
            onChange={(e) => setPrecioLista(e.target.value)}
            className={INPUT_CLASS}
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

function NuevoItemForm({
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
  const [sku, setSku] = useState("");
  const [nombre, setNombre] = useState("");
  const [unidadMedida, setUnidadMedida] = useState("");
  const [categoria, setCategoria] = useState("");
  const [stockMinimo, setStockMinimo] = useState("");
  const [precioLista, setPrecioLista] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!sku.trim() || !nombre.trim() || !unidadMedida.trim()) {
      setError("Completá todos los campos.");
      return;
    }

    let stockMinimoValue: number | undefined;
    if (stockMinimo.trim() !== "") {
      stockMinimoValue = Number(stockMinimo);
      if (Number.isNaN(stockMinimoValue) || stockMinimoValue < 0) {
        setError("El stock mínimo debe ser un número mayor o igual a cero.");
        return;
      }
    }

    let precioListaValue: number | undefined;
    if (precioLista.trim() !== "") {
      precioListaValue = Number(precioLista);
      if (Number.isNaN(precioListaValue) || precioListaValue < 0) {
        setError("El precio de lista debe ser un número mayor o igual a cero.");
        return;
      }
    }

    setSubmitting(true);
    try {
      const body: Record<string, unknown> = {
        sku: sku.trim(),
        nombre: nombre.trim(),
        unidadMedida: unidadMedida.trim(),
      };
      if (categoria.trim()) body.categoria = categoria.trim();
      if (stockMinimoValue !== undefined) body.stockMinimo = stockMinimoValue;
      if (precioListaValue !== undefined) body.precioLista = precioListaValue;
      await apiPost("/api/items", body);
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Nuevo item" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="sku" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              SKU
            </label>
            <input
              id="sku"
              value={sku}
              onChange={(e) => setSku(e.target.value)}
              className={INPUT_CLASS}
              placeholder="Ej.: LEC-01"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="nombre" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Nombre
            </label>
            <input
              id="nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className={INPUT_CLASS}
              placeholder="Ej.: Leche entera"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="unidadMedida" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Unidad de medida
            </label>
            <input
              id="unidadMedida"
              value={unidadMedida}
              onChange={(e) => setUnidadMedida(e.target.value)}
              className={INPUT_CLASS}
              placeholder="Ej.: unidad"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="categoria" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Categoría
            </label>
            <input
              id="categoria"
              value={categoria}
              onChange={(e) => setCategoria(e.target.value)}
              className={INPUT_CLASS}
              placeholder="Ej.: Harinas"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="stockMinimo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Stock mínimo
            </label>
            <input
              id="stockMinimo"
              type="number"
              min="0"
              step="0.001"
              value={stockMinimo}
              onChange={(e) => setStockMinimo(e.target.value)}
              className={INPUT_CLASS}
              placeholder="0"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="precioLista" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Precio lista
            </label>
            <input
              id="precioLista"
              type="number"
              min="0"
              step="0.01"
              value={precioLista}
              onChange={(e) => setPrecioLista(e.target.value)}
              className={INPUT_CLASS}
              placeholder="0"
            />
          </div>

          {error && <ErrorBox message={error} />}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Creando..." : "Crear item"}
            </Button>
          </div>
        </form>
    </Modal>
  );
}
