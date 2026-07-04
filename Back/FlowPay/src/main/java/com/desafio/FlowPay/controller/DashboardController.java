package com.desafio.FlowPay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.out.DashboardResumoResponse;
import com.desafio.FlowPay.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "Metricas consolidadas para o dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@Operation(summary = "Resumo do dashboard", description = "Retorna filas, atendimentos ativos, finalizados do dia, tempo medio de espera e status dos atendentes.")
	@GetMapping("/resumo")
	public DashboardResumoResponse resumo() {
		return dashboardService.resumo();
	}
}
