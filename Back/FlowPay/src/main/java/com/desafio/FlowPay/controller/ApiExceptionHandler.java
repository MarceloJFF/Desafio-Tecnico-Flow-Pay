package com.desafio.FlowPay.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.desafio.FlowPay.dto.out.ErroResponse;
import com.desafio.FlowPay.service.AtendimentoConflitoException;
import com.desafio.FlowPay.service.AtendimentoNaoEncontradoException;
import com.desafio.FlowPay.service.RequisicaoInvalidaException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(RequisicaoInvalidaException.class)
	ResponseEntity<ErroResponse> handleRequisicaoInvalida(RequisicaoInvalidaException exception) {
		return ResponseEntity.badRequest().body(new ErroResponse(exception.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErroResponse> handleJsonInvalido() {
		return ResponseEntity.badRequest().body(new ErroResponse("Payload invalido."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.badRequest().body(new ErroResponse("Parametro invalido: " + exception.getName()));
	}

	@ExceptionHandler(AtendimentoNaoEncontradoException.class)
	ResponseEntity<ErroResponse> handleNaoEncontrado(AtendimentoNaoEncontradoException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(exception.getMessage()));
	}

	@ExceptionHandler(AtendimentoConflitoException.class)
	ResponseEntity<ErroResponse> handleConflito(AtendimentoConflitoException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(exception.getMessage()));
	}
}
