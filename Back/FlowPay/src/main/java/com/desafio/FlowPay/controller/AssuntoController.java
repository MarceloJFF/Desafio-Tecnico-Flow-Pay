package com.desafio.FlowPay.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.out.AssuntoResponse;
import com.desafio.FlowPay.service.AssuntoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Assuntos", description = "Assuntos cadastrados para abertura de atendimento")
@RestController
@RequestMapping("/api/assuntos")
public class AssuntoController {

	private final AssuntoService assuntoService;

	public AssuntoController(AssuntoService assuntoService) {
		this.assuntoService = assuntoService;
	}

	@Operation(summary = "Listar assuntos", description = "Retorna os assuntos cadastrados e o time responsavel por cada um.")
	@GetMapping
	public List<AssuntoResponse> listar() {
		return assuntoService.listar();
	}
}
