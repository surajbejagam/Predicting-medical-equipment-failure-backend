# Use an official OpenJDK image as the base
FROM openjdk:17-jdk-slim as builder

# Install Maven
RUN apt-get update && \
    apt-get install -y maven python3.10 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven build files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot application
RUN mvn clean package -DskipTests

# Copy Python script and (optionally) requirements.txt and model files
# (Assume you will add predict.py and requirements.txt to the project root)
COPY ../predict.py /app/predict.py
COPY ../requirements.txt /app/requirements.txt
COPY ../medical_device_failure_risk_pipeline.pkl /app/medical_device_failure_risk_pipeline.pkl
COPY ../recall_severity_rf_no_device_dates.joblib /app/recall_severity_rf_no_device_dates.joblib

# Install Python dependencies if requirements.txt exists
RUN if [ -f requirements.txt ]; then pip3 install -r requirements.txt; fi

# Use a smaller runtime image for the final container
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy built jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Copy Python files
COPY --from=builder /app/predict.py /app/predict.py
COPY --from=builder /app/requirements.txt /app/requirements.txt
COPY --from=builder /app/medical_device_failure_risk_pipeline.pkl /app/medical_device_failure_risk_pipeline.pkl
COPY --from=builder /app/recall_severity_rf_no_device_dates.joblib /app/recall_severity_rf_no_device_dates.joblib

# Expose port (matches application.properties)
EXPOSE 8080

# Set environment variables for Python paths (update application.properties accordingly)
ENV PYTHON_EXECUTABLE=/usr/bin/python3.10
ENV PYTHON_SCRIPT_PATH=/app/predict.py

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
