package com.desafio.FlowPay.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
import com.desafio.FlowPay.dto.out.AtendimentoResponse;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.service.DistribuicaoService;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

	private final DistribuicaoService distribuicaoService;

	public AtendimentoController(DistribuicaoService distribuicaoService) {
		this.distribuicaoService = distribuicaoService;
	}

	@PostMapping
	public ResponseEntity<AtendimentoResponse> criar(@RequestBody CriarAtendimentoRequest request) {
		Atendimento atendimento = distribuicaoService.criar(request);
		return ResponseEntity
				.created(URI.create("/api/atendimentos/" + atendimento.getId()))
				.body(AtendimentoMapper.toResponse(atendimento));
	}

	@PatchMapping("/{id}/finalizar")
	public AtendimentoResponse finalizar(@PathVariable UUID id) {
		return AtendimentoMapper.toResponse(distribuicaoService.finalizar(id));
	}
}
