package com.trustworthyq.gateway.filter;

import static com.trustworthyq.gateway.filter.UserIdHeaderFilter.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserIdHeaderFilterTest {

    private final UserIdHeaderFilter filter = new UserIdHeaderFilter();

    @Test
    void addsUserIdForLoggedInUser() {
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/hello"))
                .mutate()
                .principal(Mono.just(keycloakUser("user-42")))
                .build();
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(downstream)))
                .verifyComplete();

        assertThat(downstream.get().getRequest().getHeaders().getFirst(USER_ID_HEADER))
                .isEqualTo("user-42");
    }

    @Test
    void replacesFakeUserIdFromClient() {
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/hello").header(USER_ID_HEADER, "hacker"))
                .mutate()
                .principal(Mono.just(keycloakUser("user-42")))
                .build();
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(downstream)))
                .verifyComplete();

        assertThat(downstream.get().getRequest().getHeaders().get(USER_ID_HEADER))
                .containsExactly("user-42");
    }

    @Test
    void removesFakeUserIdForAnonymousRequest() {
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/proxy/self-echo").header(USER_ID_HEADER, "hacker"));
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(downstream)))
                .verifyComplete();

        assertThat(downstream.get().getRequest().getHeaders().containsHeader(USER_ID_HEADER))
                .isFalse();
    }

    private static OAuth2AuthenticationToken keycloakUser(String sub) {
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("sub", sub));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DefaultOidcUser user = new DefaultOidcUser(authorities, idToken);
        return new OAuth2AuthenticationToken(user, authorities, "keycloak");
    }

    private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> holder) {
        return exchange -> {
            holder.set(exchange);
            return Mono.empty();
        };
    }
}