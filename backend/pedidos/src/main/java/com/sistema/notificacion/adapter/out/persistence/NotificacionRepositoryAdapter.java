package com.sistema.notificacion.adapter.out.persistence;

import com.sistema.notificacion.model.Notificacion;
import com.sistema.notificacion.port.out.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificacionRepositoryAdapter implements NotificacionRepository {

	private final NotificacionJpaRepository jpaRepository;
	private final NotificacionMapper mapper = new NotificacionMapper();

	public NotificacionRepositoryAdapter(NotificacionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Notificacion save(Notificacion n) {
		NotificacionJpaEntity entity = mapper.toJpa(n);
		NotificacionJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public List<Notificacion> findByParaUsuarioId(Long paraUsuarioId) {
		return jpaRepository.findByParaUsuarioIdOrderByFechaDesc(paraUsuarioId).stream().map(mapper::toDomain).toList();
	}
}
