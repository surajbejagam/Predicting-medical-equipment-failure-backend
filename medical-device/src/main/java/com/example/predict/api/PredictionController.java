package com.example.predict.api;

import com.example.predict.service.PythonPredictService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("v1/api/predict")
public class PredictionController {

  private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
  private final PythonPredictService service;

  public PredictionController(PythonPredictService service) {
    this.service = service;
  }

  @PostMapping("/post-binary")
  public ResponseEntity<Map<String, Object>> postBinary(@RequestBody PostBinaryRequest req) throws Exception {
    logger.info("Received POST /post-binary with payload: {}", req);
    Map<String, Object> result = service.predictPostBinary(req);
    logger.info("Response for /post-binary: {}", result);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/pre-multiclass")
  public ResponseEntity<Map<String, Object>> preMulticlass(@Valid @RequestBody PreMulticlassRequest req) throws Exception {
    logger.info("Received POST /pre-multiclass with payload: {}", req);
    Map<String, Object> result = service.predictPreMulticlass(req);
    logger.info("Response for /pre-multiclass: {}", result);
    return ResponseEntity.ok(result);
  }
}
