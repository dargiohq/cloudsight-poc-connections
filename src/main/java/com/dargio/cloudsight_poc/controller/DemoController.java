package com.dargio.cloudsight_poc.controller;

import com.dargio.cloudsight_poc.dto.UsageRequest;
import com.dargio.cloudsight_poc.service.CloudSightClient;
import com.dargio.cloudsight_poc.service.OpenAISimulator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final OpenAISimulator openAISimulator;
    private final CloudSightClient cloudSightClient;

    public DemoController(OpenAISimulator openAISimulator,
                          CloudSightClient cloudSightClient) {
        this.openAISimulator = openAISimulator;
        this.cloudSightClient = cloudSightClient;
    }

    @GetMapping("/openai")
    public String simulateOpenAI() {

        String prompt = "Write a blog about AI in healthcare";

        OpenAISimulator.Usage usage =
                openAISimulator.simulate("gpt-4", prompt);

        UsageRequest request = new UsageRequest();
        request.setService("OPENAI");
        request.setInputEndpoint("gpt-4");
        request.setOutputEndpoint("gpt-4");
        request.setInputUnits(usage.inputTokens());
        request.setOutputUnits(usage.outputTokens());

        cloudSightClient.sendUsage(request);

        return "Usage sent to Dargio CloudSight!";
    }
}