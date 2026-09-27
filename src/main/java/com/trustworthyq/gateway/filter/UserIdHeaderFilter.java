package com.trustworthyq.gateway.filter;

import java.security.Principal;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .flatMap(principal -> Mono.justOrEmpty(extractUserId(principal)))
                .map(userId -> withUserIdHeader(exchange, userId))
                .switchIfEmpty(Mono.fromSupplier(() -> withUserIdHeader(exchange, null)))
                .flatMap(chain::filter);
    }

    /**
     * Удаляет клиентский X-User-Id и, если пользователь известен, ставит настоящий.
     */
    private ServerWebExchange withUserIdHeader(ServerWebExchange exchange, String userId) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    if (userId != null) {
                        headers.set(USER_ID_HEADER, userId);
                    }
                }))
                .build();
    }

    /**
     * Достаёт ID пользователя из того, что положил Spring Security после логина.
     * Возвращает null, если пользователь не распознан.
     */
    private String extractUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return null;
        }
        Object user = authentication.getPrincipal();
        if (user instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        if (user instanceof OAuth2User oauth2User) {
            return oauth2User.getName();
        }
        return null;
    }

    @Override
    public int getOrder() {
        // Сразу после RequestIdLoggingFilter, задолго до отправки запроса в сервис
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}