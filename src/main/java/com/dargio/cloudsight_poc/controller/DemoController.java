package com.dargio.cloudsight_poc.controller;

import com.dargio.cloudsight_poc.dto.UsageRequest;
import com.dargio.cloudsight_poc.service.CloudSightClient;
import com.dargio.cloudsight_poc.service.OpenAISimulator;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
        request.setInputEndpoint("gpt-4-input");
        request.setOutputEndpoint("gpt-4-output");
        request.setInputUnits(usage.inputTokens());
        request.setOutputUnits(usage.outputTokens());

        cloudSightClient.sendUsage(request);

        return "Usage sent to Dargio CloudSight!";
    }

    @PostMapping("/seed")
    public Map<String, Object> seedUsage(
            @RequestParam(defaultValue = "240") int count,
            @RequestParam(defaultValue = "30") int days
    ) {

        if (count < 1 || count > 2000) {
            throw new IllegalArgumentException("count must be between 1 and 2000");
        }

        if (days < 1 || days > 180) {
            throw new IllegalArgumentException("days must be between 1 and 180");
        }

        List<SeedProfile> profiles = List.of(
                new SeedProfile("OPENAI", "gpt-4-input", "gpt-4-output", 8, 60, 120, 800),
                new SeedProfile("OPENAI", "gpt-4o-mini-input", "gpt-4o-mini-output", 20, 120, 80, 600),
                new SeedProfile("OPENAI", "embeddings-input", "embeddings-output", 100, 900, 20, 120),
                new SeedProfile("AWS", "lambda-input", "lambda-output", 20, 200, 10, 60),
                new SeedProfile("AWS", "s3-put", "s3-get", 10, 120, 10, 120),
                new SeedProfile("GCP", "gemini-input", "gemini-output", 12, 100, 90, 700),
                new SeedProfile("GCP", "vision-input", "vision-output", 10, 80, 40, 220)
        );

        Random random = new Random();
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            SeedProfile profile = profiles.get(random.nextInt(profiles.size()));
            UsageRequest request = new UsageRequest();

            request.setService(profile.service());
            request.setInputEndpoint(profile.inputEndpoint());
            request.setOutputEndpoint(profile.outputEndpoint());
            request.setInputUnits(randomBetween(random, profile.minInputUnits(), profile.maxInputUnits()));
            request.setOutputUnits(randomBetween(random, profile.minOutputUnits(), profile.maxOutputUnits()));
            request.setTimestamp(randomTimestamp(now, days, i, count, random));

            cloudSightClient.sendUsage(request);
        }

        return Map.of(
                "status", "OK",
                "inserted", count,
                "days", days,
                "services", List.of("OPENAI", "AWS", "GCP")
        );
    }

    private Instant randomTimestamp(Instant now, int days, int index, int total, Random random) {
        int bucket = index % 3;

        long minutesBack;
        if (bucket == 0) {
            minutesBack = random.nextInt(24 * 60);
        } else if (bucket == 1) {
            minutesBack = (24L * 60) + random.nextInt(6 * 24 * 60);
        } else {
            int remainingDays = Math.max(days - 7, 1);
            minutesBack = (7L * 24 * 60) + random.nextInt(remainingDays * 24 * 60);
        }

        long hourOffset = (long) (index % 24) * 60;

        return now
                .minus(minutesBack, ChronoUnit.MINUTES)
                .minus(hourOffset, ChronoUnit.MINUTES);
    }

    private int randomBetween(Random random, int min, int max) {
        return min + random.nextInt((max - min) + 1);
    }

    private record SeedProfile(
            String service,
            String inputEndpoint,
            String outputEndpoint,
            int minInputUnits,
            int maxInputUnits,
            int minOutputUnits,
            int maxOutputUnits
    ) {}
}
