package com.sistema.notificacion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionJpaEntity, Long> {

	List<NotificacionJpaEntity> findByParaUsuarioIdOrderByFechaDesc(Long paraUsuarioId);
}
