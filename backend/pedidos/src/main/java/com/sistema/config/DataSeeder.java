package com.sistema.config;

import com.sistema.cliente.port.in.GestionarCliente;
import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.common.model.Zona;
import com.sistema.compra.port.in.ConsultarProveedor;
import com.sistema.compra.port.in.GestionarProveedor;
import com.sistema.stock.model.Item;
import com.sistema.stock.port.in.GestionarItem;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.port.in.GestionarUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final GestionarUsuario gestionarUsuario;
	private final GestionarZona gestionarZona;
	private final GestionarCliente gestionarCliente;
	private final GestionarItem gestionarItem;
	private final RegistrarIngreso registrarIngreso;
	private final GestionarProveedor gestionarProveedor;
	private final ConsultarProveedor consultarProveedor;

	public DataSeeder(GestionarUsuario gestionarUsuario, GestionarZona gestionarZona,
			GestionarCliente gestionarCliente, GestionarItem gestionarItem, RegistrarIngreso registrarIngreso,
			GestionarProveedor gestionarProveedor, ConsultarProveedor consultarProveedor) {
		this.gestionarUsuario = gestionarUsuario;
		this.gestionarZona = gestionarZona;
		this.gestionarCliente = gestionarCliente;
		this.gestionarItem = gestionarItem;
		this.registrarIngreso = registrarIngreso;
		this.gestionarProveedor = gestionarProveedor;
		this.consultarProveedor = consultarProveedor;
	}

	@Override
	public void run(String... args) {
		seed();
	}

	public void seed() {
		seedUsuario("Admin Sistema", "admin@pedidos.com", "admin123",
				EnumSet.of(Rol.ADMINISTRATIVO, Rol.VENDEDOR, Rol.ENCARGADO_DEPOSITO, Rol.REPARTIDOR));
		seedUsuario("Repartidor Demo", "repartidor@pedidos.com", "repartidor123", EnumSet.of(Rol.REPARTIDOR));
		Long zonaCentro = obtenerZonaId("Zona Centro");
		obtenerZonaId("Zona Norte");
		Long zonaSur = obtenerZonaId("Zona Sur");
		seedCliente("Cliente Demo S.A.", "20123456789", "demo@pedidos.com", zonaCentro);
		seedCliente("Otro Cliente S.R.L.", "20987654321", "otro@pedidos.com", zonaSur);

		// Proveedor demo con su catálogo de provisión, para que la validación de
		// OC (item debe pertenecer al proveedor) sea usable desde el inicio.
		Long proveedorDemoId = seedProveedor("Distribuidora Demo S.A.", "30111111111",
				"demo@distribuidora.com", "555-0101");
		List<Long> itemIds = new ArrayList<>();
		Long harina = seedItemConIngreso("HAR-000", "Harina 000", "KG", "LOTE-DEMO-001");
		Long aceite = seedItemConIngreso("ACE-1L", "Aceite 1L", "UN", "LOTE-DEMO-002");
		Long azucar = seedItem("AZU-1K", "Azúcar 1kg", "UN");
		if (harina != null) {
			itemIds.add(harina);
		}
		if (aceite != null) {
			itemIds.add(aceite);
		}
		if (azucar != null) {
			itemIds.add(azucar);
		}
		if (proveedorDemoId != null && !itemIds.isEmpty()) {
			try {
				gestionarProveedor.setItemsDeProveedor(new GestionarProveedor.SetItemsCommand(proveedorDemoId, itemIds));
				log.info("Seed catálogo proveedor demo: {} items", itemIds.size());
			} catch (RuntimeException e) {
				log.debug("Seed catálogo no creado: {}", e.getMessage());
			}
		}
	}

	private void seedUsuario(String nombre, String email, String password, EnumSet<Rol> roles) {
		try {
			gestionarUsuario.crearUsuario(new GestionarUsuario.CrearUsuarioCommand(nombre, email, password, roles));
			log.info("Seed usuario creado: {}", email);
		} catch (RuntimeException e) {
			log.debug("Seed usuario ya existente o no creado: {} ({})", email, e.getMessage());
		}
	}

	private Long obtenerZonaId(String nombre) {
		try {
			Zona zona = gestionarZona.crearZona(new GestionarZona.CrearZonaCommand(nombre));
			log.info("Seed zona creada: {}", nombre);
			return zona.getId();
		} catch (RuntimeException e) {
			return gestionarZona.listarTodas().stream()
					.filter(z -> z.getNombre().equals(nombre))
					.findFirst()
					.map(Zona::getId)
					.orElse(null);
		}
	}

	private void seedCliente(String razonSocial, String cuit, String email, Long zonaId) {
		try {
			gestionarCliente.crearCliente(new GestionarCliente.CrearClienteCommand(razonSocial, cuit, email, null, null, zonaId));
			log.info("Seed cliente creado: {}", razonSocial);
		} catch (RuntimeException e) {
			log.debug("Seed cliente ya existente o no creado: {} ({})", razonSocial, e.getMessage());
		}
	}

	private Long seedProveedor(String razonSocial, String cuit, String email, String telefono) {
		try {
			return gestionarProveedor.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
					razonSocial, cuit, email, telefono)).getId();
		} catch (RuntimeException e) {
			return consultarProveedor.listarTodos().stream()
					.filter(p -> p.getCuit().equals(cuit))
					.findFirst()
					.map(com.sistema.compra.model.Proveedor::getId)
					.orElse(null);
		}
	}

	private Long seedItemConIngreso(String sku, String nombre, String unidad, String codigoLote) {
		try {
			Item item = gestionarItem.crearItem(new GestionarItem.CrearItemCommand(sku, nombre, unidad, null, null, null));
			registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), codigoLote, null,
					new BigDecimal("100.000"), "Seed demo", null));
			log.info("Seed item + ingreso creado: {}", sku);
			return item.getId();
		} catch (RuntimeException e) {
			log.debug("Seed item ya existente o no creado: {} ({})", sku, e.getMessage());
			return null;
		}
	}

	private Long seedItem(String sku, String nombre, String unidad) {
		try {
			Item item = gestionarItem.crearItem(new GestionarItem.CrearItemCommand(sku, nombre, unidad, null, null, null));
			log.info("Seed item creado: {}", sku);
			return item.getId();
		} catch (RuntimeException e) {
			log.debug("Seed item ya existente o no creado: {} ({})", sku, e.getMessage());
			return null;
		}
	}
}
