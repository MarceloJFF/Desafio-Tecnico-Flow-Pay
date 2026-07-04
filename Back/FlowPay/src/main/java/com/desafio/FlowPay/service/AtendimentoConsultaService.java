package com.desafio.FlowPay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendimentoRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class AtendimentoConsultaService {

	private final AtendimentoRepository atendimentoRepository;

	public AtendimentoConsultaService(AtendimentoRepository atendimentoRepository) {
		this.atendimentoRepository = atendimentoRepository;
	}

	@Transactional(readOnly = true)
	public List<Atendimento> listar(StatusAtendimento status, UUID atendenteId, TimeAtendimento time) {
		if (status == null && atendenteId == null && time == null) {
			return atendimentoRepository.findAll();
		}

		return atendimentoRepository.findAll(comFiltros(status, atendenteId, time));
	}

	private Specification<Atendimento> comFiltros(StatusAtendimento status, UUID atendenteId, TimeAtendimento time) {
		return (root, query, builder) -> {
			Predicate filtro = builder.conjunction();

			if (status != null) {
				filtro = builder.and(filtro, builder.equal(root.get("status"), status));
			}

			if (time != null) {
				filtro = builder.and(filtro, builder.equal(root.get("time"), time));
			}

			if (atendenteId != null) {
				filtro = builder.and(filtro, builder.equal(root.get("atendente").get("id"), atendenteId));
			}

			return filtro;
		};
	}
}
