package com.trustworthyq.gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;


@Component
public class RequestIdLoggingFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();

        String incomingId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        String requestId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingId;

        // Заголовок уйдёт в нижестоящий сервис
        ServerWebExchange mutated = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(REQUEST_ID_HEADER, requestId)))
                .build();

        // Заголовок вернётся клиенту в ответе
        mutated.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        return chain.filter(mutated)
                .contextWrite(context -> context.put(MDC_KEY, requestId))
                .doFinally(signal -> logAccess(mutated, requestId, startNanos));
    }

    private void logAccess(ServerWebExchange exchange, String requestId, long startNanos) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        HttpStatusCode status = exchange.getResponse().getStatusCode();

        MDC.put(MDC_KEY, requestId);
        try {
            log.info("{} {} -> {} ({} ms)",
                    exchange.getRequest().getMethod().name(),
                    exchange.getRequest().getPath().value(),
                    status != null ? status.value() : "-",
                    durationMs);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        // Выполняемся самыми первыми, чтобы ID был доступен всем остальным фильтрам
        return Ordered.HIGHEST_PRECEDENCE;
    }
}