package com.dargio.cloudsight_poc.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class OpenAISimulator {

    public Usage simulate(String model, String prompt) {

        int inputTokens = prompt.length() / 4;
        int outputTokens = new Random().nextInt(500) + 100;

        return new Usage(model, inputTokens, outputTokens);
    }

    public record Usage(String model, int inputTokens, int outputTokens) {}
}