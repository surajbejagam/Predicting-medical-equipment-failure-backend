package com.example.predict.db;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MonitorRepository extends MongoRepository<MonitorResult, String> {}
