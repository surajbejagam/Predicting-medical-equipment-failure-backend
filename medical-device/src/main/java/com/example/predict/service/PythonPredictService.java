package com.example.predict.service;

import com.example.predict.api.PostBinaryRequest;
import com.example.predict.api.PreMulticlassRequest;
import com.example.predict.db.PredictionResult;
import com.example.predict.db.PredictionResultRepository;
import com.example.predict.model.Devices;
import com.example.predict.model.Manufacturers;
import com.example.predict.repo.DevicesRepo;
import com.example.predict.repo.ManufacturersRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static ch.qos.logback.core.joran.spi.ConsoleTarget.findByName;

@Service
public class PythonPredictService {
  @Autowired
  DevicesRepo devicesRepo;
  @Autowired
  ManufacturersRepo manufacturersRepo;
  private static final Logger logger = LoggerFactory.getLogger(PythonPredictService.class);

  @Value("${python.executable}") private String pythonExe;
  @Value("${python.scriptPath}") private String scriptPath;
  @Value("${python.modelPost:}") private String modelPostPath;
  @Value("${python.modelPre:}")  private String modelPrePath;
  @Value("${python.mongoUri}")   private String mongoUri;
  @Value("${python.mongoDb}")    private String mongoDb;

  private final ObjectMapper mapper = new ObjectMapper();
  private final PredictionResultRepository repo;

  public PythonPredictService(PredictionResultRepository repo) { this.repo = repo; }

  public Map<String,Object> predictPostBinary(PostBinaryRequest req) throws Exception {
    logger.info("Starting prediction for post-binary with request: {}", req);
    List<String> cmd = new ArrayList<>();
    cmd.add(pythonExe); cmd.add(scriptPath);
    cmd.add("--task"); cmd.add("post_binary");
    if (modelPostPath != null && !modelPostPath.isBlank()) { cmd.add("--model_post"); cmd.add(modelPostPath); }
    add(cmd, "--reason", req.getReason());
    add(cmd, "--action", req.getAction());
    add(cmd, "--action_summary", req.getActionSummary());
    add(cmd, "--data_notes", req.getDataNotes());
    add(cmd, "--device_description", req.getDeviceDescription());
    add(cmd, "--device_name", req.getDeviceName());
    add(cmd, "--device_classification", req.getDeviceClassification());
    add(cmd, "--device_risk_class", req.getDeviceRiskClass());
    logger.debug("Command to execute: {}", cmd);
    Map<String, Object> result = runAndSave(cmd, "post_binary", modelPostPath);
    logger.info("Prediction result for post-binary: {}", result);
    return result;
  }

  public Map<String,Object> predictPreMulticlass(PreMulticlassRequest req) throws Exception {
    logger.info("Starting prediction for pre-multiclass with request: {}", req);
    List<String> cmd = new ArrayList<>();
    cmd.add(pythonExe); cmd.add(scriptPath);
    cmd.add("--task"); cmd.add("pre_multiclass");
    if (modelPrePath != null && !modelPrePath.isBlank()) { cmd.add("--model_pre"); cmd.add(modelPrePath); }
    cmd.add("--mongo_uri"); cmd.add(mongoUri);
    cmd.add("--mongo_db");  cmd.add(mongoDb);
    Devices device= devicesRepo.findByName(req.getDeviceName());
    add(cmd, "--device_id", device==null?"":String.valueOf(device.getId()));
    Manufacturers manufacturers =manufacturersRepo.findByName(req.getManufacturerName());
    add(cmd, "--manufacturer_id", manufacturers==null?"":String.valueOf(manufacturers.getId()));
    add(cmd, "--risk_class", req.getRiskClass());
    add(cmd, "--classification", req.getClassification());
    add(cmd, "--implanted", req.getImplanted()==null?null:req.getImplanted().toString());
    add(cmd, "--quantity_in_commerce", req.getQuantityInCommerce()==null?null:req.getQuantityInCommerce().toString());
    add(cmd, "--country", req.getCountry());
    add(cmd, "--parent_company", req.getParentCompany());
    logger.debug("Command to execute: {}", cmd);
    Map<String, Object> result = runAndSave(cmd, "pre_multiclass", modelPrePath);
    logger.info("Prediction result for pre-multiclass: {}", result);
    return result;
  }

  private static void add(List<String> cmd, String flag, String value) {
    if (value != null && !value.isBlank()) { cmd.add(flag); cmd.add(value); }
  }

  private Map<String,Object> runAndSave(List<String> cmd, String task, String modelPath) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.environment().putIfAbsent("PYTHONIOENCODING","utf-8");
    Process p = pb.start();
    String stdout = readStream(p.getInputStream());
    String stderr = readStream(p.getErrorStream());
    int code = p.waitFor();
    if (!stderr.isBlank()) System.err.println("[predict.py stderr] " + stderr);
    if (code != 0) throw new RuntimeException("predict.py failed: " + stderr);
    Map<String,Object> out = mapper.readValue(stdout, Map.class);
    PredictionResult pr = new PredictionResult();
    pr.setTask(task);
    pr.setInput(flagsToMap(cmd));
    pr.setOutput(out);
    pr.setScriptPath(scriptPath);
    pr.setModelPathUsed(modelPath);
    pr.setExitCode(code);
    pr.setStderr(stderr);
    pr.setCreatedAt(Instant.now());
    repo.save(pr);
    return out;
  }

  private static String readStream(InputStream is) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) sb.append(line).append('\n');
      return sb.toString().trim();
    }
  }

  private static Map<String,Object> flagsToMap(List<String> cmd) {
    Map<String,Object> m = new LinkedHashMap<>();
    for (int i=0;i<cmd.size();i++){
      String s = cmd.get(i);
      if (s.startsWith("--")){
        String key = s.substring(2);
        String val = (i+1<cmd.size() && !cmd.get(i+1).startsWith("--")) ? cmd.get(i+1) : "true";
        m.put(key, val);
      }
    }
    return m;
  }
}
