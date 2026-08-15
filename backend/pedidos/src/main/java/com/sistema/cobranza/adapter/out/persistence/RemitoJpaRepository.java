package com.sistema.cobranza.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemitoJpaRepository extends JpaRepository<RemitoJpaEntity, Long> {

	List<RemitoJpaEntity> findByPedidoId(Long pedidoId);

	List<RemitoJpaEntity> findByClienteId(Long clienteId);
}
