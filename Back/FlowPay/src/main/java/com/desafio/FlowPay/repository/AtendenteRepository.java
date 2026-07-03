package com.desafio.FlowPay.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.TimeAtendimento;

public interface AtendenteRepository extends JpaRepository<Atendente, UUID> {


	
	@Query(value = "SELECT pg_advisory_xact_lock(hashtext(:time))", nativeQuery = true)
	void bloquearDistribuicaoDoTime(@Param("time") String time);

	@Query(value = """
			SELECT a.* FROM atendentes a
			WHERE a.time = :time AND a.atendimentos_ativos < 3
			ORDER BY a.atendimentos_ativos ASC, a.id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	Optional<Atendente> buscarDisponivel(@Param("time") String time);

	@Query(value = "SELECT a.* FROM atendentes a WHERE a.id = :id FOR UPDATE", nativeQuery = true)
	Optional<Atendente> buscarPorIdComLock(@Param("id") UUID id);

	long countByTime(TimeAtendimento time);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Atendente a SET a.atendimentosAtivos = 0")
	void resetarAtendimentosAtivos();
}
