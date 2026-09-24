package com.trustworthyq.gateway.controller;



import com.trustworthyq.gateway.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestController {

    @GetMapping("/hello")
    public HelloResponse hello(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return new HelloResponse(
                "hello from gateway",
                requestId == null ? "none" : requestId);
    }
}