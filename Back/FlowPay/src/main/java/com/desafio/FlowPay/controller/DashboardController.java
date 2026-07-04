package com.desafio.FlowPay.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.desafio.FlowPay.dto.out.DashboardResumoResponse;
import com.desafio.FlowPay.service.DashboardStreamService;
import com.desafio.FlowPay.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "Metricas consolidadas para o dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;
	private final DashboardStreamService dashboardStreamService;

	public DashboardController(DashboardService dashboardService, DashboardStreamService dashboardStreamService) {
		this.dashboardService = dashboardService;
		this.dashboardStreamService = dashboardStreamService;
	}

	@Operation(summary = "Resumo do dashboard", description = "Retorna filas, atendimentos ativos, finalizados do dia, tempo medio de espera e status dos atendentes.")
	@GetMapping("/resumo")
	public DashboardResumoResponse resumo() {
		return dashboardService.resumo();
	}

	@Operation(summary = "Stream SSE do dashboard", description = "Mantem uma conexao SSE aberta e envia eventos de criacao, atribuicao e finalizacao de atendimentos.")
	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		return dashboardStreamService.conectar();
	}
}
