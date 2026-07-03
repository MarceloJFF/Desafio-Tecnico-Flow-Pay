package com.desafio.FlowPay.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.desafio.FlowPay.dto.out.AssuntoResponse;
import com.desafio.FlowPay.repository.AssuntoRepository;

@Service
public class AssuntoService {

	private final AssuntoRepository assuntoRepository;

	public AssuntoService(AssuntoRepository assuntoRepository) {
		this.assuntoRepository = assuntoRepository;
	}

	public List<AssuntoResponse> listar() {
		return assuntoRepository.findAll().stream()
				.map(a -> new AssuntoResponse(a.getId(), a.getNome(), a.getTime()))
				.toList();
	}
}
