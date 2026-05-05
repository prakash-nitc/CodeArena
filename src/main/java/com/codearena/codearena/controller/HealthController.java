package com.codearena.codearena.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tiny "hello world" style endpoint used to confirm the application is up.
 *
 * <p>It is the simplest possible REST handler: a method that returns a
 * {@link Map}, which Jackson serializes to a JSON object. Useful as a smoke
 * test ({@code GET /api/ping}) and as the gentlest introduction to how a
 * controller turns a Java return value into an HTTP JSON response.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        // LinkedHashMap keeps the keys in insertion order in the JSON output.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "codearena");
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
