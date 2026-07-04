package com.desafio.FlowPay.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;
	private final String[] allowedOriginPatterns;

	public CorsConfig(@Value("${flowpay.cors.allowed-origins:http://localhost:5173}") String allowedOrigins,
			@Value("${flowpay.cors.allowed-origin-patterns:}") String allowedOriginPatterns) {
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toArray(String[]::new);
		this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toArray(String[]::new);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		CorsRegistration registration = registry.addMapping("/api/**")
				.allowedMethods("GET", "POST", "PATCH", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(false);

		if (allowedOrigins.length > 0) {
			registration.allowedOrigins(allowedOrigins);
		}

		if (allowedOriginPatterns.length > 0) {
			registration.allowedOriginPatterns(allowedOriginPatterns);
		}
	}
}
