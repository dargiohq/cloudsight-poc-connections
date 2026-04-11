package com.dargio.cloudsight_poc.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UsageRequest {
    private String service;
    private String inputEndpoint;
    private String outputEndpoint;
    private int inputUnits;
    private int outputUnits;
    private Instant timestamp;
}
