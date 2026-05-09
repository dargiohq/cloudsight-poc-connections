package com.dargio.cloudsight_poc.controller;

import com.dargio.cloudsight_poc.service.CloudSightConnectionClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final CloudSightConnectionClient cloudSightConnectionClient;

    public DemoController(CloudSightConnectionClient cloudSightConnectionClient) {
        this.cloudSightConnectionClient = cloudSightConnectionClient;
    }

    @GetMapping("/contract")
    public Map<String, Object> contract() {
        return cloudSightConnectionClient.contract();
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return cloudSightConnectionClient.overview();
    }

    @PostMapping("/register-all")
    public Map<String, Object> registerAll() {
        return cloudSightConnectionClient.registerAll();
    }

    @PostMapping("/register/{provider}")
    public Map<String, Object> registerProvider(@PathVariable String provider) {
        return cloudSightConnectionClient.registerProvider(provider);
    }

    @PostMapping("/validate-all")
    public List<Map<String, Object>> validateAll() {
        return cloudSightConnectionClient.validateAll();
    }

    @PostMapping("/validate/{id}")
    public Map<String, Object> validateOne(@PathVariable UUID id) {
        return cloudSightConnectionClient.validate(id);
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit() {
        return cloudSightConnectionClient.audit();
    }
}
