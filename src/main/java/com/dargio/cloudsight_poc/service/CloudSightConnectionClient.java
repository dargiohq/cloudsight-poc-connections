package com.dargio.cloudsight_poc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudSightConnectionClient {

    @Value("${cloudsight.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final CloudSightAuthClient cloudSightAuthClient;
    private final AuditTrailService auditTrailService;

    public CloudSightConnectionClient(CloudSightAuthClient cloudSightAuthClient, AuditTrailService auditTrailService) {
        this.cloudSightAuthClient = cloudSightAuthClient;
        this.auditTrailService = auditTrailService;
    }

    public Map<String, Object> contract() {
        return Map.of(
                "integrationOption", "billing-connection-first",
                "authentication", "Workspace admin JWT from /auth/login",
                "connectionEndpoint", baseUrl + "/api/connections",
                "validationEndpoint", baseUrl + "/api/connections/{id}/validate",
                "noPiiGuidance", List.of(
                        "Use provider, billing, account, project, or subscription identifiers only.",
                        "Store secrets by reference, not raw values, wherever possible.",
                        "Do not include user emails, customer names, or prompts in notes or identifiers."
                ),
                "providers", List.of("AWS", "GCP", "AZURE", "OPENAI")
        );
    }

    public Map<String, Object> overview() {
        return exchange(baseUrl + "/api/connections", HttpMethod.GET, null);
    }

    public Map<String, Object> registerAll() {
        List<String> providers = List.of("AWS", "GCP", "AZURE", "OPENAI");
        List<Map<String, Object>> results = providers.stream()
                .map(this::registerProvider)
                .toList();
        return Map.of(
                "integrationOption", "billing-connection-first",
                "registeredProviders", providers,
                "results", results,
                "overview", overview()
        );
    }

    public Map<String, Object> registerProvider(String provider) {
        String normalizedProvider = provider.toUpperCase(Locale.ROOT);
        Map<String, Object> currentOverview = overview();
        List<Map<String, Object>> currentConnections = (List<Map<String, Object>>) currentOverview.getOrDefault("connections", List.of());

        for (Map<String, Object> connection : currentConnections) {
            if (normalizedProvider.equalsIgnoreCase(String.valueOf(connection.get("provider")))) {
                return Map.of(
                        "status", "EXISTS",
                        "provider", normalizedProvider,
                        "connection", connection
                );
            }
        }

        Map<String, Object> requestBody = connectionTemplate(normalizedProvider);
        return exchange(baseUrl + "/api/connections", HttpMethod.POST, requestBody);
    }

    public List<Map<String, Object>> validateAll() {
        Map<String, Object> currentOverview = overview();
        List<Map<String, Object>> currentConnections = (List<Map<String, Object>>) currentOverview.getOrDefault("connections", List.of());
        return currentConnections.stream()
                .map(connection -> validate(UUID.fromString(String.valueOf(connection.get("id")))))
                .toList();
    }

    public Map<String, Object> validate(UUID id) {
        return exchange(baseUrl + "/api/connections/" + id + "/validate", HttpMethod.POST, Map.of());
    }

    public List<Map<String, Object>> audit() {
        return auditTrailService.events();
    }

    private Map<String, Object> exchange(String url, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cloudSightAuthClient.token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, method, entity, Map.class);

        Map<String, Object> auditEvent = new LinkedHashMap<>();
        auditEvent.put("integrationOption", "billing-connection-first");
        auditEvent.put("method", method.name());
        auditEvent.put("url", url);
        auditEvent.put("headers", Map.of(
                "Authorization", "Bearer REDACTED",
                "Content-Type", "application/json"
        ));
        auditEvent.put("requestBody", body == null ? Map.of() : body);
        auditEvent.put("responseStatus", response.getStatusCode().value());
        auditEvent.put("responseBody", response.getBody());
        auditTrailService.record(auditEvent);

        return response.getBody();
    }

    private Map<String, Object> connectionTemplate(String provider) {
        return switch (provider) {
            case "AWS" -> Map.of(
                    "provider", "AWS",
                    "connectionName", "aws-prod-finops",
                    "authType", "IAM role",
                    "accountIdentifier", "aws-prod-billing-01",
                    "projectIdentifier", "aws-prod-organization",
                    "secretReference", "env:CLOUDSIGHT_AWS_CONNECTION_SECRET",
                    "status", "CONFIGURED",
                    "environment", "Production",
                    "scopes", List.of("cur-export", "ec2-pricing", "rds-pricing", "cloudfront-pricing"),
                    "notes", "No PII. Billing metadata and provider pricing scope only."
            );
            case "GCP" -> Map.of(
                    "provider", "GCP",
                    "connectionName", "gcp-prod-finops",
                    "authType", "Service account",
                    "accountIdentifier", "gcp-billing-ops-01",
                    "projectIdentifier", "gcp-platform-prod",
                    "secretReference", "env:CLOUDSIGHT_GCP_CONNECTION_SECRET",
                    "status", "CONFIGURED",
                    "environment", "Production",
                    "scopes", List.of("billing-export", "cloud-run", "bigquery", "vertex-ai"),
                    "notes", "No PII. Billing account, project scope, and provider metadata only."
            );
            case "AZURE" -> Map.of(
                    "provider", "AZURE",
                    "connectionName", "azure-prod-finops",
                    "authType", "Service principal",
                    "accountIdentifier", "azure-subscription-prod-01",
                    "projectIdentifier", "azure-tenant-core",
                    "secretReference", "env:CLOUDSIGHT_AZURE_CONNECTION_SECRET",
                    "status", "CONFIGURED",
                    "environment", "Production",
                    "scopes", List.of("cost-management", "azure-openai", "functions", "sql"),
                    "notes", "No PII. Subscription and tenant identifiers only."
            );
            case "OPENAI" -> Map.of(
                    "provider", "OPENAI",
                    "connectionName", "openai-prod-org",
                    "authType", "API key",
                    "accountIdentifier", "openai-org-core",
                    "projectIdentifier", "openai-enterprise-prod",
                    "secretReference", "env:CLOUDSIGHT_OPENAI_CONNECTION_SECRET",
                    "status", "CONFIGURED",
                    "environment", "Production",
                    "scopes", List.of("usage.read", "models.read", "pricing.snapshot"),
                    "notes", "No PII. Organization-level usage and pricing metadata only."
            );
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }
}
