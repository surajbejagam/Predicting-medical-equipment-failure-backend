package com.example.predict.api;

import com.example.predict.service.StatusSummaryService;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("v1/api/status-summary")
public class StatusSummaryController {

    private final StatusSummaryService service;

    public StatusSummaryController(StatusSummaryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> buildSummary(@RequestBody Map<String, String> req) {
        String country = req.get("country");
        String deviceName = req.get("deviceName");

        if (country == null || deviceName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Need country and deviceName"));
        }

        Document result = service.getOrBuildSummary(country, deviceName);
        return ResponseEntity.ok(result);
    }
}