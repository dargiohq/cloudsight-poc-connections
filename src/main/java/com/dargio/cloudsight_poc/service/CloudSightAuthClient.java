package com.dargio.cloudsight_poc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudSightAuthClient {

    @Value("${cloudsight.api.base-url}")
    private String baseUrl;

    @Value("${cloudsight.workspace.email}")
    private String workspaceEmail;

    @Value("${cloudsight.workspace.password}")
    private String workspacePassword;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AuditTrailService auditTrailService;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public CloudSightAuthClient(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    public synchronized String token() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }

        String loginUrl = baseUrl + "/auth/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of(
                "email", workspaceEmail,
                "password", workspacePassword
        ), headers);

        ResponseEntity<Map> response = restTemplate.exchange(loginUrl, HttpMethod.POST, entity, Map.class);
        Map<String, Object> body = response.getBody();
        cachedToken = body == null ? null : String.valueOf(body.get("token"));
        expiresAt = Instant.now().plusSeconds(45 * 60);

        Map<String, Object> auditEvent = new LinkedHashMap<>();
        auditEvent.put("integrationOption", "billing-connection-first");
        auditEvent.put("scenario", "workspace-authentication");
        auditEvent.put("method", "POST");
        auditEvent.put("url", loginUrl);
        auditEvent.put("headers", Map.of("Content-Type", "application/json"));
        auditEvent.put("requestBody", Map.of("email", "REDACTED", "password", "REDACTED"));
        auditEvent.put("responseStatus", response.getStatusCode().value());
        auditEvent.put("responseBody", Map.of("token", cachedToken == null ? "" : "REDACTED", "apiKey", body == null ? "" : "REDACTED"));
        auditTrailService.record(auditEvent);

        return cachedToken;
    }
}
