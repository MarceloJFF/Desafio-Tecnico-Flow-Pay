package com.desafio.FlowPay.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.desafio.FlowPay.dto.out.DashboardEventResponse;

@Service
public class DashboardStreamService {

	private static final Long TIMEOUT_SEM_LIMITE = 0L;

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public SseEmitter conectar() {
		SseEmitter emitter = new SseEmitter(TIMEOUT_SEM_LIMITE);
		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(erro -> emitters.remove(emitter));

		enviarConectado(emitter);
		return emitter;
	}

	@EventListener
	public void enviar(DashboardEventResponse evento) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
						.name(evento.tipo())
						.data(evento));
			}
			catch (IOException | IllegalStateException exception) {
				emitters.remove(emitter);
			}
		}
	}

	private void enviarConectado(SseEmitter emitter) {
		try {
			emitter.send(SseEmitter.event()
					.name("dashboard-conectado")
					.data("ok"));
		}
		catch (IOException | IllegalStateException exception) {
			emitters.remove(emitter);
		}
	}
}
