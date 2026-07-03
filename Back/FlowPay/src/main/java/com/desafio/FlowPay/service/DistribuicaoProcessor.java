package com.desafio.FlowPay.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendenteRepository;
import com.desafio.FlowPay.repository.AtendimentoRepository;

@Service
public class DistribuicaoProcessor {

	private final AtendimentoRepository atendimentoRepository;
	private final AtendenteRepository atendenteRepository;

	public DistribuicaoProcessor(AtendimentoRepository atendimentoRepository, AtendenteRepository atendenteRepository) {
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteRepository = atendenteRepository;
	}

	@Transactional
	public boolean tentarAtribuirProximo(TimeAtendimento time) {
		atendenteRepository.bloquearDistribuicaoDoTime(time.name());

		Atendente atendente = atendenteRepository.buscarDisponivel(time.name()).orElse(null);
		if (atendente == null) {
			return false;
		}
		if (atendente.getAtendimentosAtivos() >= 3) {
			return false;
		}

		Atendimento proximo = atendimentoRepository.buscarProximoDaFila(time.name()).orElse(null);
		if (proximo == null) {
			return false;
		}

		proximo.setAtendente(atendente);
		proximo.setStatus(StatusAtendimento.EM_ATENDIMENTO);
		proximo.setAtribuidoEm(LocalDateTime.now());
		atendente.setAtendimentosAtivos(atendente.getAtendimentosAtivos() + 1);
		return true;
	}
}
