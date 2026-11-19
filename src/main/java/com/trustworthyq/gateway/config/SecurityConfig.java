package com.trustworthyq.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint.DelegateEntry;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        var homeRedirect = new RedirectServerAuthenticationSuccessHandler("/");
        homeRedirect.setRequestCache(NoOpServerRequestCache.getInstance());
        var entryPoint = new DelegatingServerAuthenticationEntryPoint(new DelegateEntry(
                ServerWebExchangeMatchers.pathMatchers("/api/**"),
                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)));
        entryPoint.setDefaultEntryPoint(new RedirectServerAuthenticationEntryPoint("/login"));
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/self-echo", "/proxy/**").permitAll()
                        .pathMatchers("/", "/login", "/register", "/assets/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .oauth2Login(login -> login.loginPage("/login")
                        .authenticationSuccessHandler(homeRedirect))
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }
}
