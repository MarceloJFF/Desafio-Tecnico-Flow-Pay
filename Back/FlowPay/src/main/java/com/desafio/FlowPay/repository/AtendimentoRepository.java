package com.desafio.FlowPay.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;

public interface AtendimentoRepository extends JpaRepository<Atendimento, UUID>, JpaSpecificationExecutor<Atendimento> {

	@Query(value = "SELECT a.* FROM atendimentos a WHERE a.id = :id FOR UPDATE", nativeQuery = true)
	Optional<Atendimento> buscarPorIdComLock(@Param("id") UUID id);

	@Query(value = """
			SELECT a.* FROM atendimentos a
			WHERE a.time = :time AND a.status = 'AGUARDANDO'
			ORDER BY a.criado_em ASC, a.id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	Optional<Atendimento> buscarProximoDaFila(@Param("time") String time);

	List<Atendimento> findByStatus(StatusAtendimento status);

	List<Atendimento> findByAtendenteId(UUID atendenteId);

	List<Atendimento> findByAtendenteIdAndStatus(UUID atendenteId, StatusAtendimento status);

	List<Atendimento> findByAtribuidoEmIsNotNull();

	List<Atendimento> findByStatusAndTimeOrderByCriadoEmAsc(StatusAtendimento status, TimeAtendimento time);

	long countByStatusAndTime(StatusAtendimento status, TimeAtendimento time);

	long countByStatusAndFinalizadoEmBetween(StatusAtendimento status, java.time.LocalDateTime inicio,
			java.time.LocalDateTime fim);
}
