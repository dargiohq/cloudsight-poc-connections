package com.dargio.cloudsight_poc.service;

import com.dargio.cloudsight_poc.dto.UsageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CloudSightClient {

    @Value("${dargio.api.url}")
    private String apiUrl;

    @Value("${dargio.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendUsage(UsageRequest request) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UsageRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(apiUrl, entity, String.class);

        System.out.println("CloudSight Response: " + response.getBody());
    }
}