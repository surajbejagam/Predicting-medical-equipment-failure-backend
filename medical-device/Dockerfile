# ============================
# Builder Stage
# ============================
FROM openjdk:17-jdk-slim as builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package spring-boot:repackage

COPY ../predict.py /app/predict.py
COPY ../requirements.txt /app/requirements.txt
COPY ../medical_device_failure_risk_pipeline.pkl /app/medical_device_failure_risk_pipeline.pkl
COPY ../recall_severity_rf_no_device_dates.joblib /app/recall_severity_rf_no_device_dates.joblib

RUN pip3 install --no-cache-dir -r requirements.txt || true

# ============================
# Runtime Stage
# ============================
FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        python3 \
        python3-pip && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/medical-device-predictor-1.0.0.jar app.jar
COPY --from=builder /app/predict.py /app/predict.py
COPY --from=builder /app/requirements.txt /app/requirements.txt
COPY --from=builder /app/medical_device_failure_risk_pipeline.pkl /app/medical_device_failure_risk_pipeline.pkl
COPY --from=builder /app/recall_severity_rf_no_device_dates.joblib /app/recall_severity_rf_no_device_dates.joblib

RUN pip3 install --no-cache-dir -r requirements.txt || true

EXPOSE 8080

ENV PYTHON_EXECUTABLE=/usr/bin/python3
ENV PYTHON_SCRIPT_PATH=/app/predict.py

ENTRYPOINT ["java", "-jar", "app.jar"]
