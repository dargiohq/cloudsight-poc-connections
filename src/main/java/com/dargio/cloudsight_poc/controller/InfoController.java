package com.dargio.cloudsight_poc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "application", "cloudsight-poc-connections",
                "mode", "connection-first",
                "health", "/health",
                "contract", "/demo/contract",
                "overview", "/demo/overview",
                "registerAll", "/demo/register-all",
                "audit", "/demo/audit"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "application", "cloudsight-poc-connections"
        );
    }
}
