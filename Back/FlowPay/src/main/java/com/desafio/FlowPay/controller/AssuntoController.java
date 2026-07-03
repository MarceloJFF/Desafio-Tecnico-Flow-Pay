package com.desafio.FlowPay.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.out.AssuntoResponse;
import com.desafio.FlowPay.service.AssuntoService;

@RestController
@RequestMapping("/api/assuntos")
public class AssuntoController {

	private final AssuntoService assuntoService;

	public AssuntoController(AssuntoService assuntoService) {
		this.assuntoService = assuntoService;
	}

	@GetMapping
	public List<AssuntoResponse> listar() {
		return assuntoService.listar();
	}
}
