package com.example.predict.service;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatusSummaryService {

    private final MongoTemplate mongo;

    private final String devicesCol = "devices";
    private final String eventsCol = "events";
    private final String manufacturersCol = "manufacturers";
    private final String monitorCol = "monitor_status"; // cache collection

    public StatusSummaryService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Document getOrBuildSummary(String country, String deviceName) {
        String key = country + "||" + deviceName;

        // 1. Check cache
        Document cached = mongo.getCollection(monitorCol)
                .find(new Document("_id", key)).first();
        if (cached != null) return cached;

        // 2. Find device
        Document device = mongo.getCollection(devicesCol)
                .find(new Document("name", new Document("$regex", deviceName).append("$options", "i"))
                        .append("country", country))
                .first();
        if (device == null) return new Document("error", "Device not found");

        Object deviceId = device.get("id");

        // 3. Find events
        List<Document> events = mongo.getCollection(eventsCol)
                .find(new Document("device_id", deviceId).append("country", country))
                .limit(200)
                .into(new ArrayList<>());

        // 4. Status counts
        Map<String, Long> statusCounts = events.stream()
                .collect(Collectors.groupingBy(
                        e -> normalizeStatus(e.get("status")),
                        Collectors.counting()
                ));

        // 5. Top 5 events
        List<Document> top5 = events.stream().limit(5).map(e -> new Document()
                .append("id", e.get("id"))
                .append("action", e.get("action"))
                .append("status", normalizeStatus(e.get("status")))
                .append("date", e.get("date"))
        ).collect(Collectors.toList());

        // 6. Manufacturers
        Set<String> mfrs =events.stream()
                .map(e -> e.get("device_id"))
                .filter(Objects::nonNull)
                .map(mid -> {
                    Document m = mongo.getCollection(manufacturersCol).find(new Document("id", mid)).first();
                    return m != null ? m.getString("name") : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 7. Build summary
        Document summary = new Document("_id", key)
                .append("country", country)
                .append("deviceName", deviceName)
                .append("statusCounts", statusCounts)
                .append("top5Events", top5)
                .append("manufacturers", new ArrayList<>(mfrs));

        // 8. Save
        mongo.getCollection(monitorCol).insertOne(summary);

        return summary;
    }

    private String normalizeStatus(Object raw) {
        if (raw == null) return "Unknown";
        String s = raw.toString().trim();
        if (s.equalsIgnoreCase("Terminated")) return "Terminated";
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return "Terminated"; // date string
        return s;
    }
}
