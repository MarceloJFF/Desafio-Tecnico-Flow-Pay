package com.desafio.FlowPay.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.out.AtendimentoResponse;
import com.desafio.FlowPay.dto.out.AtendenteStatusResponse;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.service.AtendimentoConsultaService;
import com.desafio.FlowPay.service.AtendenteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Atendentes", description = "Consulta de atendentes e ocupacao atual")
@RestController
@RequestMapping("/api/atendentes")
public class AtendenteController {

	private final AtendenteService atendenteService;
	private final AtendimentoConsultaService atendimentoConsultaService;

	public AtendenteController(AtendenteService atendenteService, AtendimentoConsultaService atendimentoConsultaService) {
		this.atendenteService = atendenteService;
		this.atendimentoConsultaService = atendimentoConsultaService;
	}

	@Operation(summary = "Listar atendentes", description = "Retorna todos os atendentes com time e quantidade de atendimentos ativos.")
	@GetMapping
	public List<AtendenteStatusResponse> listar() {
		return atendenteService.listar();
	}

	@Operation(summary = "Listar atendimentos do atendente", description = "Retorna todos os atendimentos de um atendente, incluindo finalizados. Aceita filtro opcional por status.")
	@GetMapping("/{id}/atendimentos")
	public List<AtendimentoResponse> listarAtendimentos(
			@PathVariable UUID id,
			@Parameter(description = "Status para filtro: AGUARDANDO, EM_ATENDIMENTO ou FINALIZADO")
			@RequestParam(required = false) StatusAtendimento status) {
		return atendimentoConsultaService.listar(status, id, null).stream()
				.map(AtendimentoMapper::toResponse)
				.toList();
	}
}
