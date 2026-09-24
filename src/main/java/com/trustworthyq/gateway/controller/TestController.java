package com.trustworthyq.gateway.controller;


import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestController {

    @GetMapping("/hello")
    public Map<String, String> hello(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Map.of(
                "message", "hello from gateway",
                "receivedRequestId", requestId == null ? "none" : requestId);
    }
}