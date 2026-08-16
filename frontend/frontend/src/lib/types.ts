export type EstadoPedido =
  | "PENDIENTE_CONFIRMACION"
  | "PENDIENTE_PREPARACION"
  | "PENDIENTE_STOCK"
  | "PENDIENTE_ENTREGA"
  | "EN_VIAJE"
  | "ENTREGADO"
  | "ENTREGADO_PARCIAL"
  | "RE_AGENDADO"
  | "RECHAZADO";

export type TipoMovimiento =
  | "INGRESO"
  | "RESERVA_PEDIDO"
  | "LIBERACION_RESERVA"
  | "EGRESO_VENTA"
  | "MERMA"
  | "AJUSTE_INVENTARIO";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  usuarioId: number;
  email: string;
  roles: string[];
}

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  roles: string[];
}

export interface Zona {
  id: number;
  nombre: string;
  activo: boolean;
}

export interface Cliente {
  id: number;
  razonSocial: string;
  cuit: string;
  email: string | null;
  telefono: string | null;
  domicilio: string | null;
  zonaId: number | null;
  zonaNombre: string | null;
  activo: boolean;
}

export interface Proveedor {
  id: number;
  razonSocial: string;
  cuit: string;
  email: string | null;
  telefono: string | null;
  activo: boolean;
}

export interface Item {
  id: number;
  sku: string;
  nombre: string;
  categoriaId: number | null;
  categoriaNombre: string | null;
  unidadMedida: string;
  stockMinimo: number;
  precioLista: number;
  activo: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  activo: boolean;
}

export interface StockInfo {
  itemId: number;
  sku: string;
  itemNombre: string;
  disponible: number;
  reservasActivas: number;
}

export interface MovimientoStock {
  id: number;
  tipo: TipoMovimiento;
  itemId: number;
  loteId: number | null;
  pedidoId: number | null;
  cantidad: number;
  fecha: string;
  motivo: string | null;
}

export interface Lote {
  id: number;
  itemId: number;
  proveedorId: number | null;
  codigoLote: string;
  fechaIngreso: string;
  fechaVencimiento: string | null;
  cantidadIngresada: number;
  disponible: number;
  estado: "VENCIDO" | "AGOTADO" | "VIGENTE" | "DESCARTADO";
  itemNombre: string | null;
  itemSku: string | null;
}

export interface IngresoRequest {
  itemId: number;
  codigoLote?: string;
  fechaVencimiento?: string;
  cantidad: number;
  motivo?: string;
}

export interface MermaRequest {
  itemId: number;
  loteId: number;
  cantidad: number;
  motivo: string;
}

export interface AjusteRequest {
  itemId: number;
  cantidad: number;
  motivo: string;
}

export interface PedidoItem {
  pedidoItemId: number;
  itemId: number;
  cantidadPedida: number;
  cantidadReservada: number;
  cantidadEntregada: number;
  precioUnitario: number;
  pendienteStock: boolean;
}

export interface Pedido {
  id: number;
  numero: string;
  clienteId: number;
  vendedorId: number;
  pedidoPadreId: number | null;
  estado: EstadoPedido;
  fechaCreacion: string;
  updatedAt: string;
  fechaJornada: string | null;
  observaciones: string | null;
  total: number;
  express: boolean;
  items: PedidoItem[];
}

export interface CrearPedidoRequest {
  clienteId: number;
  vendedorId: number;
  fechaJornada?: string;
  observaciones?: string;
  items: { itemId: number; cantidad: number; precioUnitario: number }[];
}

export type EstadoOrdenCompra = "PENDIENTE" | "RECIBIDA_PARCIAL" | "RECIBIDA" | "CANCELADA";

export interface OrdenCompraLinea {
  id: number;
  itemId: number;
  cantidadPedida: number;
  cantidadRecibida: number;
  precioUnitario: number;
  restante: number;
}

export interface OrdenCompra {
  id: number;
  numero: string;
  proveedorId: number;
  fecha: string;
  estado: EstadoOrdenCompra;
  observaciones: string | null;
  lineas: OrdenCompraLinea[];
}

export interface CrearOrdenCompraRequest {
  proveedorId: number;
  observaciones?: string;
  lineas: { itemId: number; cantidad: number; precioUnitario: number }[];
}

export interface RecepcionRequest {
  lineas: { lineaId: number; cantidadRecibida: number }[];
}

export interface ReporteStockItem {
  itemId: number;
  sku: string;
  nombre: string;
  disponible: number;
  reservasActivas: number;
}

export interface ReporteVenta {
  vendedorId: number;
  vendedorNombre: string;
  cantidadPedidos: number;
  cantidadUnidades: number;
  monto: number;
}

export type EstadoRuta = "PLANIFICADA" | "EN_CURSO" | "FINALIZADA";

export interface Ruta {
  id: number;
  zonaId: number;
  repartidorId: number;
  fechaJornada: string;
  estado: EstadoRuta;
  pedidoIds: number[];
  capacidadBultos: number;
}

export interface RutaRequest {
  zonaId: number;
  repartidorId: number;
  fechaJornada: string;
  pedidoIds: number[];
  capacidadBultos: number;
}

export interface AsignarPedidosRequest {
  pedidoIds: number[];
}

export interface EntregaLineaRequest {
  pedidoItemId: number;
  cantidadEntregada: number;
}

export interface EntregaRequest {
  entregas: EntregaLineaRequest[];
}

export type FormaPago = "EFECTIVO" | "TRANSFERENCIA" | "TARJETA" | "OTRO";

export interface Cobranza {
  id: number;
  clienteId: number;
  pedidoId: number | null;
  monto: number;
  formaPago: FormaPago;
  fecha: string;
  observaciones: string | null;
}

export interface EstadoCuenta {
  clienteId: number;
  totalVendido: number;
  totalCobrado: number;
  saldo: number;
}

export interface RemitoLinea {
  id: number;
  itemId: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Remito {
  id: number;
  numero: string;
  pedidoId: number;
  clienteId: number;
  fechaEmision: string;
  montoTotal: number;
  lineas: RemitoLinea[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
}

export interface PorFormaPago {
  formaPago: string;
  monto: number;
  cantidad: number;
}

export interface PorDia {
  fecha: string;
  monto: number;
  cantidad: number;
}

export interface PorVendedor {
  vendedorId: number;
  vendedorNombre: string;
  monto: number;
  cantidad: number;
}

export interface ResumenCaja {
  totalCobrado: number;
  cantidadCobranzas: number;
  porFormaPago: PorFormaPago[];
  porDia: PorDia[];
  porVendedor: PorVendedor[];
}

export interface Sustitucion {
  id: number;
  pedidoId: number;
  itemOriginalId: number;
  itemSustitutoId: number;
  cantidad: number;
  diferenciaPrecio: number;
  fecha: string;
  observaciones: string | null;
}

export interface Notificacion {
  id: number;
  tipo: string;
  mensaje: string;
  paraUsuarioId: number;
  pedidoId: number | null;
  leida: boolean;
  fecha: string;
}

export interface NoLeidasResponse {
  cantidad: number;
}
