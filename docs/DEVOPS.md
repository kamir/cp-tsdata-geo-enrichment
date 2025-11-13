# DevOps Guide: cp-tsdata-geo-enrichment

## Table of Contents

1. [Build and Deployment](#build-and-deployment)
2. [Environment Configuration](#environment-configuration)
3. [Container Management](#container-management)
4. [Kafka Operations](#kafka-operations)
5. [Monitoring and Observability](#monitoring-and-observability)
6. [Troubleshooting](#troubleshooting)
7. [CI/CD Integration](#cicd-integration)
8. [Operational Runbooks](#operational-runbooks)

---

## Build and Deployment

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 8+ | Runtime environment |
| Maven | 3.x | Build tool |
| Docker | 19.03+ | Containerization (optional) |
| Git | 2.x | Version control |
| Confluent Cloud Account | - | Kafka cluster access |

### Build Process

#### Local Maven Build

```bash
# Full build with dependencies
mvn clean compile package install

# Output: target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar (16 MB)
```

**Build Stages**:
1. **Clean**: Remove previous build artifacts
2. **Compile**: Java 8 compilation with strict linting (`-Xlint:all`)
3. **Package**: Create fat JAR with Maven Shade plugin
4. **Install**: Deploy to local Maven repository

**Key Build Artifacts**:
- `target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar` - Executable fat JAR
- `target/dependency-reduced-pom.xml` - Shade plugin output (excludes signed dependencies)

#### Build Configuration

**pom.xml** highlights:

```xml
<properties>
    <java.version>8</java.version>
    <kafka.version>2.6.0</kafka.version>
    <confluent.version>6.0.0</confluent.version>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <configuration>
        <transformers>
            <transformer implementation="...ManifestResourceTransformer">
                <mainClass>tool.SimulationScenario</mainClass>
            </transformer>
        </transformers>
        <filters>
            <filter>
                <artifact>*:*</artifact>
                <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                </excludes>
            </filter>
        </filters>
    </configuration>
</plugin>
```

**Why Fat JAR?**
- Single executable artifact
- All dependencies bundled (Kafka clients, serializers, Gson)
- Simplifies deployment (no dependency management on target hosts)

#### Build Scripts

**`bin/run_locally.sh`**:

```bash
#!/bin/bash
cd ..
mvn clean compile package install
java -jar ./target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/
```

**`bin/release.sh`**:

```bash
#!/bin/bash
export APP_NAME=cp-tsdata-demo-2
export APP_VERSION=v1

cd ..

# Save environment variables
echo "export APP_NAME=$APP_NAME" > ops/cloud-app-instances/$APP_NAME/released-app/cfg/env.sh
echo "export APP_VERSION=$APP_VERSION" >> ops/cloud-app-instances/$APP_NAME/released-app/cfg/env.sh

# Build application
mvn clean compile package install

# Copy artifacts
cp target/*.jar ops/cloud-app-instances/$APP_NAME/released-apps

# Build Docker image
docker build . -t $APP_NAME

# Copy schemas for ksqlDB
cp ./src/main/avro/*.avsc ops/cloud-app-instances/$APP_NAME/released-app/kst-context/schemas
```

### Deployment Strategies

#### 1. Local Deployment

**Use Case**: Development, testing, demos

```bash
# Step 1: Configure credentials
cp ccloud.props.secure ccloud.props
nano ccloud.props  # Add your Kafka credentials

# Step 2: Build
mvn clean package

# Step 3: Run
java -jar ./target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/
```

**Validation**:
- Check console output for successful CSV loading
- Verify Kafka topics created in Confluent Cloud UI
- Inspect `data/out/` for GeoJSON files

#### 2. Docker Deployment

**Use Case**: Consistent runtime environments, cloud deployments

**Build Image**:

```bash
docker build . -t cp-tsdata-demo:v1
```

**Run Container**:

```bash
# Basic run
docker run --rm cp-tsdata-demo:v1

# With volume mounts (for custom data)
docker run --rm \
  -v $(pwd)/data:/app/cp-tsdata-geo-enrichment/data \
  -v $(pwd)/ccloud.props:/app/cp-tsdata-geo-enrichment/ccloud.props \
  cp-tsdata-demo:v1

# With custom data path
docker run --rm cp-tsdata-demo:v1 \
  java -jar /app/cp-tsdata-geo-enrichment/target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar \
  /path/to/custom/data/
```

**Dockerfile Explained**:

```dockerfile
FROM gradle:jdk8                          # Base image with Java 8

RUN mkdir /app && mkdir /app/cp-tsdata-geo-enrichment

# Clone source code (includes sample data)
RUN git clone https://github.com/kamir/cp-tsdata-geo-enrichment.git /app/cp-tsdata-geo-enrichment

# Copy pre-built JAR (skips build for faster image creation)
COPY target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar /app/cp-tsdata-geo-enrichment/target/

# Copy credentials
COPY ccloud.props.secure /app/cp-tsdata-geo-enrichment/ccloud.props

WORKDIR /app/cp-tsdata-geo-enrichment/

# Execute application
CMD exec java -jar /app/cp-tsdata-geo-enrichment/target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar /app/cp-tsdata-geo-enrichment/data/in/
```

**Optimization Recommendations**:
- Use multi-stage build to reduce image size
- Cache Maven dependencies in separate layer
- Use `.dockerignore` to exclude unnecessary files

#### 3. Cloud Deployment

**Use Case**: Production-like environments, scheduled jobs

**Target Platform** (inferred from `ops/` structure):
- Kubernetes (implied by ops/cloud-app-instances structure)
- VM-based deployment
- Managed container services (AWS ECS, GCP Cloud Run)

**Release Process**:

```bash
# Step 1: Build and package
./bin/release.sh

# Step 2: Push Docker image to registry
docker tag cp-tsdata-demo-2:v1 gcr.io/YOUR_PROJECT/cp-tsdata-demo-2:v1
docker push gcr.io/YOUR_PROJECT/cp-tsdata-demo-2:v1

# Step 3: Deploy to cloud environment
# (Kubernetes example)
kubectl apply -f ops/cloud-app-instances/cp-tsdata-demo-2/deployment.yaml
```

**Cloud Deployment Considerations**:
- Use Kubernetes Secrets for `ccloud.props` credentials
- Implement readiness/liveness probes (application must expose health endpoints)
- Configure resource limits (memory: 512Mi-1Gi, CPU: 0.5-1 core)
- Use init containers to validate Kafka connectivity before starting main app

---

## Environment Configuration

### Configuration Files

#### `ccloud.props` (Kafka Configuration)

**Template** (`ccloud.props.secure`):

```properties
# Kafka
bootstrap.servers=YOUR_CLUSTER.confluent.cloud:9092
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username='API_KEY' password='API_SECRET';
sasl.mechanism=PLAIN

# Confluent Cloud Schema Registry
schema.registry.url=https://YOUR_SR.confluent.cloud
basic.auth.credentials.source=USER_INFO
schema.registry.basic.auth.user.info=SR_KEY:SR_SECRET
```

**Production Configuration**:

```bash
# Create from template
cp ccloud.props.secure ccloud.props

# Add credentials (use environment-specific values)
# DEV, STAGING, PROD environments should have separate Kafka clusters
```

**Best Practices**:
- ✅ Never commit `ccloud.props` to version control
- ✅ Use separate Kafka clusters per environment
- ✅ Rotate API keys quarterly
- ✅ Use environment variables in containerized deployments
- ✅ Encrypt credentials at rest (e.g., AWS Secrets Manager, HashiCorp Vault)

#### Environment Variables

**Recommended Approach**:

```bash
# Set credentials via environment variables
export KAFKA_BOOTSTRAP_SERVERS="pkc-xxx.confluent.cloud:9092"
export KAFKA_API_KEY="<your-api-key>"
export KAFKA_API_SECRET="<your-api-secret>"
export SCHEMA_REGISTRY_URL="https://psrc-xxx.confluent.cloud"
export SCHEMA_REGISTRY_KEY="<sr-key>"
export SCHEMA_REGISTRY_SECRET="<sr-secret>"

# Modify application to read from environment (requires code changes)
```

**Docker Environment Variables**:

```bash
docker run --rm \
  -e KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
  -e KAFKA_API_KEY="$KAFKA_API_KEY" \
  -e KAFKA_API_SECRET="$KAFKA_API_SECRET" \
  cp-tsdata-demo:v1
```

**Kubernetes Secrets**:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kafka-credentials
type: Opaque
stringData:
  ccloud.props: |
    bootstrap.servers=pkc-xxx.confluent.cloud:9092
    security.protocol=SASL_SSL
    sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username='API_KEY' password='API_SECRET';
    sasl.mechanism=PLAIN
    schema.registry.url=https://psrc-xxx.confluent.cloud
    basic.auth.credentials.source=USER_INFO
    schema.registry.basic.auth.user.info=SR_KEY:SR_SECRET
```

Mount as volume:

```yaml
volumes:
  - name: kafka-config
    secret:
      secretName: kafka-credentials
volumeMounts:
  - name: kafka-config
    mountPath: /app/cp-tsdata-geo-enrichment/ccloud.props
    subPath: ccloud.props
```

---

## Container Management

### Docker Best Practices

#### Multi-Stage Dockerfile (Recommended)

```dockerfile
# Build stage
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar app.jar
COPY data /app/data
COPY ccloud.props /app/ccloud.props

ENTRYPOINT ["java", "-jar", "app.jar", "/app/data/in/"]
```

**Benefits**:
- Smaller image size (JRE vs JDK)
- Separate build dependencies from runtime
- Faster layer caching

#### Health Checks

**Add to Dockerfile**:

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD java -cp /app/app.jar tool.HealthCheck || exit 1
```

**Implement `tool.HealthCheck.java`**:

```java
public class HealthCheck {
    public static void main(String[] args) {
        // Check Kafka connectivity
        // Verify CSV files exist
        // Return exit code 0 for healthy, 1 for unhealthy
    }
}
```

### Container Registry

**Push to Registry**:

```bash
# Docker Hub
docker tag cp-tsdata-demo:v1 username/cp-tsdata-demo:v1
docker push username/cp-tsdata-demo:v1

# AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
docker tag cp-tsdata-demo:v1 123456789.dkr.ecr.us-east-1.amazonaws.com/cp-tsdata-demo:v1
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/cp-tsdata-demo:v1

# GCP Container Registry
docker tag cp-tsdata-demo:v1 gcr.io/project-id/cp-tsdata-demo:v1
docker push gcr.io/project-id/cp-tsdata-demo:v1
```

### Kubernetes Deployment

**`ops/cloud-app-instances/cp-tsdata-demo-2/deployment.yaml`** (example):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cp-tsdata-demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cp-tsdata-demo
  template:
    metadata:
      labels:
        app: cp-tsdata-demo
    spec:
      containers:
      - name: simulator
        image: gcr.io/project-id/cp-tsdata-demo:v1
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        volumeMounts:
        - name: kafka-config
          mountPath: /app/ccloud.props
          subPath: ccloud.props
      volumes:
      - name: kafka-config
        secret:
          secretName: kafka-credentials
```

**Deploy**:

```bash
kubectl apply -f ops/cloud-app-instances/cp-tsdata-demo-2/deployment.yaml
kubectl get pods -l app=cp-tsdata-demo
kubectl logs -f <pod-name>
```

---

## Kafka Operations

### Topic Management

**List Topics**:

```bash
# Using Confluent CLI
confluent kafka topic list --cluster lkc-xxxxx

# Using kafka-topics command
kafka-topics --bootstrap-server pkc-xxx.confluent.cloud:9092 \
  --command-config ccloud.props \
  --list | grep cp-tsdata
```

**Expected Topics** (appID=demo3):

- `cp-tsdata.demo3.grid-regions`
- `cp-tsdata.demo3.grid-stations`
- `cp-tsdata.demo3.grid-plants`
- `cp-tsdata.demo3.grid-links`
- `cp-tsdata.demo3.grid-link-flow-data`

**Topic Configuration**:

```bash
# View topic details
confluent kafka topic describe cp-tsdata.demo3.grid-link-flow-data --cluster lkc-xxxxx

# Expected configuration:
# - Partitions: 1 (default, increase for higher throughput)
# - Replication Factor: 3 (Confluent Cloud default)
# - Retention: 7 days (default)
```

**Increase Partitions**:

```bash
confluent kafka topic update cp-tsdata.demo3.grid-link-flow-data \
  --partitions 3 \
  --cluster lkc-xxxxx
```

### Consumer Verification

**Consume Recent Messages**:

```bash
# Using Confluent CLI
confluent kafka topic consume cp-tsdata.demo3.grid-link-flow-data \
  --cluster lkc-xxxxx \
  --from-beginning \
  --max-messages 10

# Using kafka-console-consumer
kafka-console-consumer --bootstrap-server pkc-xxx.confluent.cloud:9092 \
  --consumer.config ccloud.props \
  --topic cp-tsdata.demo3.grid-link-flow-data \
  --from-beginning \
  --max-messages 10
```

**Expected Output**:

```json
{"linkId":"L1","timestamp":1634567890,"flowMW":450.23}
{"linkId":"L2","timestamp":1634567891,"flowMW":320.15}
...
```

### Schema Registry Operations

**List Schemas**:

```bash
curl -u SR_KEY:SR_SECRET https://psrc-xxx.confluent.cloud/subjects
```

**Expected Schemas** (if Avro serialization used):

- `cp-tsdata.demo3.grid-link-flow-data-value`

**View Schema**:

```bash
curl -u SR_KEY:SR_SECRET \
  https://psrc-xxx.confluent.cloud/subjects/cp-tsdata.demo3.grid-link-flow-data-value/versions/latest
```

---

## Monitoring and Observability

### Application Metrics

**Current State**: Minimal (console output only)

**Console Output Monitoring**:

```bash
# Monitor stdout for errors
java -jar app.jar ./data/in/ 2>&1 | tee application.log

# Search for errors
grep -i "error\|exception\|failed" application.log
```

### Kafka Producer Metrics

**JMX Metrics** (exposed by Kafka client library):

```bash
# Enable JMX in JVM
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar app.jar ./data/in/
```

**Key Metrics**:
- `kafka.producer:type=producer-metrics,client-id=*` - Producer throughput
- `kafka.producer:type=producer-topic-metrics,client-id=*,topic=*` - Per-topic metrics
- `record-send-rate` - Records sent per second
- `record-error-rate` - Failed send attempts

**Monitor with JConsole**:

```bash
jconsole localhost:9999
```

### Confluent Cloud Monitoring

**Metrics Dashboard**:
- Navigate to Confluent Cloud UI → Cluster → Metrics
- Monitor:
  - Throughput (bytes/sec, records/sec)
  - Request rate
  - Consumer lag (if downstream consumers exist)

**Alerts** (recommended):
- Producer errors > 0
- Consumer lag > 1000 messages
- Cluster disk usage > 80%

### Logging

**Current Implementation**:

```java
System.out.println("> Read model files from: " + CSVFileRepository.repoPath);
```

**Recommended Enhancement**:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(SimulationScenario.class);

logger.info("Read model files from: {}", CSVFileRepository.repoPath);
logger.error("Failed to load CSV file", exception);
```

**Logback Configuration** (`src/main/resources/logback.xml`):

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

**Centralized Logging**:

```bash
# Ship logs to ELK stack
java -jar app.jar ./data/in/ | filebeat -e -c filebeat.yml

# Or use Docker logging driver
docker run --log-driver=awslogs \
  --log-opt awslogs-group=cp-tsdata-demo \
  cp-tsdata-demo:v1
```

---

## Troubleshooting

### Common Issues

#### 1. Kafka Connection Failures

**Symptom**:

```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms.
```

**Root Causes**:
- Invalid credentials in `ccloud.props`
- Network connectivity issues
- Incorrect bootstrap servers

**Resolution**:

```bash
# Test connectivity
curl -I https://pkc-xxx.confluent.cloud:9092

# Verify credentials
confluent login
confluent kafka cluster list

# Check ccloud.props format
cat ccloud.props | grep -E "bootstrap.servers|sasl.jaas.config"
```

#### 2. CSV File Not Found

**Symptom**:

```
Exception in thread "main" java.io.FileNotFoundException: ./data/in/E-Grid - Sheet1.csv
```

**Root Causes**:
- Incorrect data path argument
- Missing CSV files

**Resolution**:

```bash
# Verify data directory structure
ls -la ./data/in/

# Ensure all required CSV files exist
ls -1 ./data/in/*.csv
# Expected: E-Grid - Sheet1.csv, Sheet2.csv, Sheet3.csv

# Run with correct path
java -jar app.jar /absolute/path/to/data/in/
```

#### 3. Schema Registry Errors

**Symptom**:

```
io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException: Unauthorized; error code: 401
```

**Root Causes**:
- Invalid Schema Registry credentials
- Incorrect authentication configuration

**Resolution**:

```bash
# Test Schema Registry connectivity
curl -u SR_KEY:SR_SECRET https://psrc-xxx.confluent.cloud/subjects

# Verify ccloud.props
grep "schema.registry" ccloud.props
# Expected:
# schema.registry.url=https://psrc-xxx.confluent.cloud
# schema.registry.basic.auth.user.info=KEY:SECRET
```

#### 4. Out of Memory Errors

**Symptom**:

```
java.lang.OutOfMemoryError: Java heap space
```

**Root Causes**:
- Insufficient heap size for large datasets
- Memory leaks in simulation loop

**Resolution**:

```bash
# Increase heap size
java -Xmx2g -jar app.jar ./data/in/

# Monitor memory usage
jstat -gc <pid> 1000

# Enable heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -jar app.jar ./data/in/
```

### Debugging

**Enable Debug Logging**:

```bash
# Add Kafka client debug logging
export KAFKA_OPTS="-Dlog4j.configuration=file:log4j.properties"

# log4j.properties:
log4j.rootLogger=INFO, stdout
log4j.logger.org.apache.kafka=DEBUG
```

**Trace Network Requests**:

```bash
# Capture Kafka protocol traffic
tcpdump -i any -A -s 0 'tcp port 9092'

# Or use Wireshark with SASL_SSL decryption
```

**Maven Dependency Analysis**:

```bash
# Check for dependency conflicts
mvn dependency:tree

# Resolve conflicts
mvn dependency:analyze
```

---

## CI/CD Integration

### GitHub Actions (Example)

**`.github/workflows/build.yml`**:

```yaml
name: Build and Test

on:
  push:
    branches: [ main, claude/* ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 8
      uses: actions/setup-java@v3
      with:
        java-version: '8'
        distribution: 'temurin'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Build with Maven
      run: mvn clean package -DskipTests

    - name: Run tests
      run: mvn test

    - name: Build Docker image
      run: docker build . -t cp-tsdata-demo:${{ github.sha }}

    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: application-jar
        path: target/*.jar
```

**`.github/workflows/deploy.yml`** (Example):

```yaml
name: Deploy to Production

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Build and push Docker image
      env:
        DOCKER_REGISTRY: gcr.io/your-project
      run: |
        docker build . -t $DOCKER_REGISTRY/cp-tsdata-demo:${{ github.ref_name }}
        docker push $DOCKER_REGISTRY/cp-tsdata-demo:${{ github.ref_name }}

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/cp-tsdata-demo \
          simulator=$DOCKER_REGISTRY/cp-tsdata-demo:${{ github.ref_name }}
```

### GitLab CI (Example)

**`.gitlab-ci.yml`**:

```yaml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository

build:
  stage: build
  image: maven:3.8-openjdk-8
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar

test:
  stage: test
  image: maven:3.8-openjdk-8
  script:
    - mvn test

deploy:
  stage: deploy
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build . -t $CI_REGISTRY_IMAGE:$CI_COMMIT_TAG
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_TAG
  only:
    - tags
```

---

## Operational Runbooks

### Runbook 1: Deploy New Version

**Objective**: Deploy new application version to production

**Steps**:

```bash
# 1. Checkout and build
git checkout main
git pull origin main
mvn clean package

# 2. Run local smoke test
java -jar target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/
# Verify: Check console output for errors

# 3. Build Docker image
docker build . -t cp-tsdata-demo:v1.1.0

# 4. Push to registry
docker push gcr.io/project-id/cp-tsdata-demo:v1.1.0

# 5. Update Kubernetes deployment
kubectl set image deployment/cp-tsdata-demo simulator=gcr.io/project-id/cp-tsdata-demo:v1.1.0

# 6. Monitor rollout
kubectl rollout status deployment/cp-tsdata-demo

# 7. Verify Kafka topics
confluent kafka topic consume cp-tsdata.demo3.grid-link-flow-data --cluster lkc-xxxxx --max-messages 5

# 8. Check application logs
kubectl logs -f deployment/cp-tsdata-demo
```

**Rollback**:

```bash
kubectl rollout undo deployment/cp-tsdata-demo
```

### Runbook 2: Rotate Kafka Credentials

**Objective**: Update Kafka API keys without downtime

**Steps**:

```bash
# 1. Create new API key in Confluent Cloud
confluent api-key create --resource lkc-xxxxx

# 2. Update Kubernetes secret
kubectl create secret generic kafka-credentials-new \
  --from-literal=KAFKA_API_KEY=<new-key> \
  --from-literal=KAFKA_API_SECRET=<new-secret> \
  --dry-run=client -o yaml | kubectl apply -f -

# 3. Update deployment to use new secret
kubectl patch deployment cp-tsdata-demo -p '{"spec":{"template":{"spec":{"volumes":[{"name":"kafka-config","secret":{"secretName":"kafka-credentials-new"}}]}}}}'

# 4. Trigger rolling restart
kubectl rollout restart deployment/cp-tsdata-demo

# 5. Verify new credentials work
kubectl logs -f deployment/cp-tsdata-demo | grep -i "kafka"

# 6. Delete old API key
confluent api-key delete <old-key-id>
```

### Runbook 3: Increase Throughput

**Objective**: Scale application to handle higher data volumes

**Steps**:

```bash
# 1. Increase topic partitions
confluent kafka topic update cp-tsdata.demo3.grid-link-flow-data --partitions 6 --cluster lkc-xxxxx

# 2. Scale Kubernetes deployment
kubectl scale deployment cp-tsdata-demo --replicas=3

# 3. Monitor producer metrics
kubectl exec -it <pod-name> -- jconsole

# 4. Verify message distribution across partitions
confluent kafka topic describe cp-tsdata.demo3.grid-link-flow-data --cluster lkc-xxxxx
```

---

## Summary

This DevOps guide covers:

- ✅ **Build and Deployment**: Maven build, Docker containerization, cloud deployment
- ✅ **Configuration Management**: Kafka credentials, environment variables, secrets
- ✅ **Monitoring**: Application metrics, Kafka producer metrics, logging
- ✅ **Troubleshooting**: Common issues and resolutions
- ✅ **CI/CD**: GitHub Actions and GitLab CI examples
- ✅ **Runbooks**: Operational procedures for common tasks

**Next Steps**:
- Implement health checks and readiness probes
- Add structured logging with SLF4J
- Set up alerting for Kafka producer errors
- Create automated deployment pipelines

See `IMPROVEMENTS.md` for additional recommendations.
