package com.desafio.FlowPay.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.dto.out.AtendenteStatusResponse;
import com.desafio.FlowPay.repository.AtendenteRepository;

@Service
public class AtendenteService {

	private final AtendenteRepository atendenteRepository;

	public AtendenteService(AtendenteRepository atendenteRepository) {
		this.atendenteRepository = atendenteRepository;
	}

	@Transactional(readOnly = true)
	public List<AtendenteStatusResponse> listar() {
		return atendenteRepository.findAll().stream()
				.map(atendente -> new AtendenteStatusResponse(
						atendente.getId(),
						atendente.getNome(),
						atendente.getTime(),
						atendente.getAtendimentosAtivos()))
				.toList();
	}
}
