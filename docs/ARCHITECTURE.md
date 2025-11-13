# Architecture Documentation: cp-tsdata-geo-enrichment

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Component Design](#component-design)
3. [Data Models](#data-models)
4. [Data Flow](#data-flow)
5. [Integration Architecture](#integration-architecture)
6. [Technology Choices](#technology-choices)
7. [Design Patterns](#design-patterns)
8. [Scalability Considerations](#scalability-considerations)

---

## System Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    cp-tsdata-geo-enrichment                      │
│                     (Java Application)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Data       │    │   Domain     │    │   Kafka      │      │
│  │   Provider   │───▶│   Model      │───▶│  Producers   │      │
│  │   Layer      │    │   Layer      │    │   Layer      │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│         │                    │                    │              │
│         ▼                    ▼                    ▼              │
│   ┌──────────┐        ┌──────────┐         ┌──────────┐        │
│   │   CSV    │        │  Graph   │         │  Topic   │        │
│   │  Files   │        │  Model   │         │  Group   │        │
│   └──────────┘        └──────────┘         └──────────┘        │
│                             │                                    │
│                             ▼                                    │
│                    ┌──────────────┐                             │
│                    │   GeoJSON    │                             │
│                    │   Exporter   │                             │
│                    └──────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Confluent Cloud │
                    │   Kafka Topics   │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │     ksqlDB       │
                    │  Stream Processor│
                    └──────────────────┘
```

### Layered Architecture

The application follows a **layered architecture** pattern:

| Layer | Responsibility | Key Components |
|-------|----------------|----------------|
| **Presentation** | Entry point, orchestration | `SimulationScenario.main()` |
| **Business Logic** | Domain models, graph operations | `datamodel.*`, `GridFactory` |
| **Data Access** | CSV file I/O | `CSVFileRepository`, `GridDataProvider` |
| **Integration** | Kafka publishing, GeoJSON export | `kafka.dataassets.*`, `GeoJSONExporter` |

---

## Component Design

### 1. Data Provider Layer

**Purpose**: Abstract data source operations and provide grid configuration

#### `CSVFileRepository`

```java
// Responsibilities:
- Load CSV files from filesystem
- Parse CSV into domain objects
- Manage repository path configuration

// Key Methods:
+ getPowerPlantsFromRepository(): Vector<PowerPlant>
+ getStationsFromRepository(): Vector<Station>
+ getRegionsFromRepository(): Vector<Region>
```

**Location**: `src/main/java/dataprovider/CSVFileRepository.java`

**Design Notes**:
- Static utility class pattern
- No dependency injection (hardcoded file paths)
- CSV parsing logic embedded in repository

#### `GridDataProvider`

```java
// Responsibilities:
- Provide static grid configuration
- Define segment metadata

// Key Methods:
+ getSegments(): Vector<String>
```

**Location**: `src/main/java/dataprovider/GridDataProvider.java`

---

### 2. Domain Model Layer

**Purpose**: Represent power grid entities and their relationships

#### Class Hierarchy

```
Node (abstract)
├── PowerPlant
├── Station
└── Region

Relation (abstract)
├── GridLink
└── InterNodeLink

Measurement
└── PowerFlowSample

POI
└── POIData
```

#### Core Entities

##### `Node` (Abstract Base Class)

```java
// Attributes:
- id: String
- name: String
- latitude: double
- longitude: double

// Methods:
+ asPoi(): POIData  // Convert to Point of Interest
```

##### `PowerPlant` extends `Node`

```java
// Additional Attributes:
- capacityMW: double
- linkedToStation: String  // Foreign key to Station

// Location: src/main/java/datamodel/graph_of_things/nodes/PowerPlant.java
```

##### `Station` extends `Node`

```java
// Additional Attributes:
- region: String  // Foreign key to Region

// Location: src/main/java/datamodel/graph_of_things/nodes/Station.java
```

##### `Region` extends `Node`

```java
// Additional Attributes:
- production: double    // Total generation capacity (MW)
- consumption: double   // Total load (MW)
- imports: double       // Inter-regional imports (MW)
- exports: double       // Inter-regional exports (MW)

// Location: src/main/java/datamodel/graph_of_things/nodes/Region.java
```

##### `GridLink` (Edge/Relation)

```java
// Attributes:
- id: String
- source: Station
- target: Station
- nominalFlow: double      // Expected power flow (MW)
- epsilon: double          // Variance for simulation

// Methods:
+ newSample(iteration: int): PowerFlowSample  // Generate time series sample

// Location: src/main/java/datamodel/graph_of_things/relations/GridLink.java
```

##### `PowerFlowSample` (Measurement)

```java
// Attributes:
- linkId: String
- timestamp: long
- flowMW: double           // Measured flow with noise

// Location: src/main/java/datamodel/measurement/PowerFlowSample.java
```

---

### 3. Kafka Integration Layer

**Purpose**: Publish grid data and measurements to Kafka topics

#### `TopicGroupTool` (Orchestrator)

```java
// Responsibilities:
- Initialize all producers
- Coordinate topic publishing
- Manage producer lifecycle

// Key Methods:
+ configureProducer(appID: String): void
+ storeRegionContextData(regions: Vector<Region>): void
+ storeStationContextData(stations: Vector<Station>): void
+ storePlantContextData(plants: Vector<PowerPlant>): void
+ storeLinkContextData(links: Vector<GridLink>): void
```

**Location**: `src/main/java/kafka/dataassets/TopicGroupTool.java`

#### Producer Components

Each producer handles a specific topic:

| Producer | Topic Suffix | Record Type | Purpose |
|----------|--------------|-------------|---------|
| `RegionProducer` | `grid-regions` | `Region` | Regional boundaries |
| `StationProducer` | `grid-stations` | `Station` | Transformer stations |
| `PlantProducer` | `grid-plants` | `PowerPlant` | Generation facilities |
| `GridLinkProducer` | `grid-links` | `GridLink` | Network topology |
| `PowerSampleProducer` | `grid-link-flow-data` | `PowerFlowSample` | Time series measurements |

**Location**: `src/main/java/kafka/dataassets/producers/*.java`

#### `GenericProducerFactory`

```java
// Responsibilities:
- Abstract Kafka producer creation
- Load configuration from ccloud.props
- Support multiple serialization formats (JSON, Avro)

// Design Pattern: Factory Method
```

**Location**: `src/main/java/kafka/GenericProducerFactory.java`

---

### 4. GeoJSON Export Layer

**Purpose**: Generate geospatial representations for visualization

#### `GeoJSONExporter`

```java
// Responsibilities:
- Generate GeoJSON FeatureCollections
- Export nodes (stations, plants) as Point features
- Export links as LineString features

// Key Methods:
+ generateGrid(): void              // Station network topology
+ generateRegionLinks(): void       // Inter-regional connections
+ initExportFolder(path: String): void

// Output Files:
- nodes.geojson          // All nodes as points
- grid.json              // Station network
- links-result.json      // Regional connections
```

**Location**: `src/main/java/tool/geojson/GeoJSONExporter.java`

**GeoJSON Format**:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [longitude, latitude]
      },
      "properties": {
        "id": "S1",
        "name": "Station Name",
        "type": "Station"
      }
    }
  ]
}
```

---

### 5. Orchestration Layer

**Purpose**: Coordinate application execution flow

#### `SimulationScenario` (Main Entry Point)

```java
// Execution Flow:
1. Load CSV data (stations, plants, regions)
2. Generate GeoJSON visualizations
3. Configure Kafka producers
4. Publish context data to Kafka
5. Simulate power flow iterations
6. Validate energy balance

// Key State:
- powerPlants: Vector<PowerPlant>
- stations: Vector<Station>
- regions: Vector<Region>
- gridLinks: Vector<GridLink>     // Derived from stations
- segments: Vector<String>        // Static configuration

// Configuration:
- appID: String = "demo3"
```

**Location**: `src/main/java/tool/SimulationScenario.java:21`

**Execution Sequence Diagram**:

```
SimulationScenario.main(args)
    │
    ├─▶ CSVFileRepository.getPowerPlantsFromRepository()
    ├─▶ CSVFileRepository.getStationsFromRepository()
    ├─▶ CSVFileRepository.getRegionsFromRepository()
    ├─▶ GridDataProvider.getSegments()
    │
    ├─▶ GeoJSONExporter.generateGrid()
    ├─▶ GeoJSONExporter.generateRegionLinks()
    │
    ├─▶ TopicGroupTool.configureProducer(appID)
    ├─▶ TopicGroupTool.storeRegionContextData(regions)
    ├─▶ TopicGroupTool.storeStationContextData(stations)
    ├─▶ TopicGroupTool.storePlantContextData(powerPlants)
    ├─▶ TopicGroupTool.storeLinkContextData(gridLinks)
    │
    └─▶ simulateFlow()
           │
           └─▶ Loop 10 iterations:
                  ├─▶ For each GridLink:
                  │      ├─▶ GridLink.newSample(iteration)
                  │      └─▶ PowerSampleProducer.sendSample(sample)
                  │
                  ├─▶ PowerSampleProducer.flush()
                  └─▶ calcBalanceForRegion(regions)
```

---

## Data Models

### Entity-Relationship Diagram

```
┌─────────────┐         ┌─────────────┐
│   Region    │◀───────▶│   Station   │
│             │ 1     * │             │
└─────────────┘         └─────────────┘
                              │ 1
                              │
                              │ *
                        ┌─────────────┐
                        │ PowerPlant  │
                        └─────────────┘

┌─────────────┐         ┌─────────────┐
│   Station   │◀────────▶│  GridLink   │◀────────┐
│  (Source)   │          │             │         │
└─────────────┘          └─────────────┘         │
                              │                  │
                              │ 1                │
                              │                  │
                              │ *                │
                        ┌──────────────────┐    │
                        │ PowerFlowSample  │    │
                        └──────────────────┘    │
                                                 │
                        ┌─────────────┐         │
                        │   Station   │─────────┘
                        │  (Target)   │
                        └─────────────┘
```

### Data Relationships

| Relationship | Cardinality | Description |
|--------------|-------------|-------------|
| Region → Station | 1:N | Each station belongs to one region |
| Station → PowerPlant | 1:N | Each plant is connected to one station |
| Station → Station | N:N | Stations connected via GridLinks |
| GridLink → PowerFlowSample | 1:N | Each link generates multiple samples over time |

---

## Data Flow

### Context Data Publication (Startup)

```
CSV Files → CSVFileRepository → Domain Models → Kafka Producers → Kafka Topics
    │             │                    │              │               │
  Sheet1       Parse              PowerPlant     JSON/Avro      grid-plants
  Sheet2       Parse              Station        Serialize      grid-stations
  Sheet3       Parse              Region         Encode         grid-regions
                │                    │              │               │
                └────────────────────┴──────────────┴───────────────┘
                                     │
                            Confluent Cloud Kafka
                                     │
                                 ksqlDB Tables
```

### Streaming Data Publication (Runtime)

```
GridLink.newSample()
    │
    ├─▶ nominalFlow ± random(epsilon)  // White noise simulation
    │
    ├─▶ PowerFlowSample(linkId, timestamp, flowMW)
    │
    └─▶ PowerSampleProducer.sendSample()
            │
            ├─▶ JSON Serialization
            │
            └─▶ Kafka Topic: grid-link-flow-data
                    │
                    └─▶ ksqlDB Stream
                            │
                            └─▶ Analytics / Dashboards
```

### GeoJSON Export Flow

```
Domain Models → GeoJSONExporter → GeoJSON Files → Web Viewer
    │                  │               │              │
PowerPlant        Feature          nodes.geojson   Browser
Station           Collection       grid.json       Map Render
Region            Builder          links.json
    │                  │               │
    └──────────────────┴───────────────┘
                       │
              https://utahemre.github.io/geojsontest.html
```

---

## Integration Architecture

### External Dependencies

#### Confluent Cloud Kafka

**Connection Configuration**: `ccloud.props`

```properties
bootstrap.servers=pkc-4yyd6.us-east1.gcp.confluent.cloud:9092
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required...
```

**Topic Naming Convention**: `cp-tsdata.{appID}.{entity}`

Example: `cp-tsdata.demo3.grid-link-flow-data`

#### Schema Registry

**Purpose**: Avro schema validation and evolution

**Configuration**:

```properties
schema.registry.url=https://psrc-4v1qj.eu-central-1.aws.confluent.cloud
basic.auth.credentials.source=USER_INFO
schema.registry.basic.auth.user.info=USER:SECRET
```

**Note**: Avro schemas defined in `src/main/avro/*.avsc`

---

## Technology Choices

### Build and Dependency Management

**Maven** (pom.xml)

**Key Plugins**:
- `maven-compiler-plugin` (3.8.1) - Java 8 compilation
- `maven-shade-plugin` (3.2.4) - Fat JAR creation with dependency bundling

**Shade Configuration**:
- Main class: `tool.SimulationScenario`
- Filters: Exclude signed JARs (*.SF, *.DSA, *.RSA)

### Serialization

**Gson** (2.7)
- JSON object construction for GeoJSON
- Simple, lightweight

**org.json** (20090211)
- JSON manipulation utilities
- Legacy but stable

**Kafka Serializers**:
- `JsonSerializer` (Confluent 6.0.0) - For domain objects
- `AvroSerializer` (Confluent 5.3.3) - For schema-validated records

### Graph Processing (Commented Out)

**JUNG** (2.0.1) - Java Universal Network/Graph Framework

**Status**: Dependencies commented out in pom.xml

**Rationale**: Likely used in earlier development phases for network analysis, but current implementation uses simple Vector-based collections

---

## Design Patterns

### 1. Factory Pattern

**`GenericProducerFactory`**

```java
// Creates Kafka producers with consistent configuration
public static KafkaProducer<String, T> createProducer() {
    Properties props = loadConfig("ccloud.props");
    return new KafkaProducer<>(props);
}
```

**Benefits**:
- Centralized producer configuration
- Consistent security settings
- Easy to switch between environments

### 2. Repository Pattern

**`CSVFileRepository`**

```java
// Abstracts data source operations
public static Vector<Station> getStationsFromRepository() {
    // CSV parsing logic encapsulated
}
```

**Limitations**:
- No interface abstraction
- Tightly coupled to CSV format
- Static methods prevent dependency injection

### 3. Builder Pattern (Implicit)

**`GeoJSONExporter`**

```java
// Constructs complex GeoJSON structures incrementally
JSONObject feature = new JSONObject();
feature.put("type", "Feature");
feature.put("geometry", geometryObject);
feature.put("properties", propertiesObject);
```

### 4. Facade Pattern

**`TopicGroupTool`**

```java
// Simplifies interaction with multiple producers
TopicGroupTool.configureProducer(appID);
TopicGroupTool.storeRegionContextData(regions);
TopicGroupTool.storeStationContextData(stations);
```

**Benefits**:
- Single entry point for Kafka operations
- Hides producer complexity
- Coordinates related operations

---

## Scalability Considerations

### Current Limitations

| Aspect | Current State | Impact |
|--------|---------------|--------|
| **Threading** | Single-threaded execution | Cannot parallelize CSV loading or Kafka publishing |
| **Memory** | All data loaded into memory (Vectors) | Limited by heap size (~9 stations × 5 plants × 10 iterations) |
| **I/O** | Synchronous file and network operations | Blocking execution |
| **Producer** | One producer per topic | Manual lifecycle management |
| **Simulation** | Fixed 10 iterations | Not suitable for long-running scenarios |

### Scalability Improvements (See IMPROVEMENTS.md)

**Horizontal Scaling**:
- ❌ Not supported (stateful CSV loading)
- ✅ Kafka handles downstream scalability

**Vertical Scaling**:
- ✅ Increase heap size: `java -Xmx2g -jar ...`
- ✅ Add more stations/plants to CSV files

**Performance Optimizations**:
- Use `KafkaProducer` batch configuration
- Implement async CSV parsing
- Stream-based processing instead of loading all data

---

## Security Architecture

### Authentication

**Kafka**: SASL/PLAIN over TLS

```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
```

**Schema Registry**: HTTP Basic Auth

```properties
basic.auth.credentials.source=USER_INFO
schema.registry.basic.auth.user.info=USER:SECRET
```

### Credential Management

**Current Approach**:
- Credentials stored in `ccloud.props` (plaintext)
- `.gitignore` excludes `ccloud.props` (but not enforced)
- `ccloud.props.secure` serves as template

**Security Risks**:
- ⚠️ Credentials in plaintext file
- ⚠️ No encryption at rest
- ⚠️ No secrets rotation mechanism

**Recommendations** (See IMPROVEMENTS.md):
- Use environment variables
- Integrate with HashiCorp Vault or AWS Secrets Manager
- Implement credential rotation

---

## Testing Architecture

**Current State**: Minimal

**JUnit 4.12** dependency present, but:
- No unit tests in `/src/test/`
- No integration tests
- No mocking framework

**Testing Strategy** (See IMPROVEMENTS.md):
- Unit tests for domain models
- Integration tests for Kafka producers
- End-to-end tests for simulation scenarios

---

## Deployment Architecture

### Local Development

```
Developer Machine
├── Java 8 JRE
├── Maven 3.x
├── source code
└── ccloud.props (credentials)
    │
    └─▶ java -jar target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/
```

### Dockerized Deployment

```
Docker Host
├── Dockerfile
│   ├── gradle:jdk8 base image
│   ├── Git clone repo
│   ├── Maven build (optional)
│   └── Copy pre-built JAR
│
└── docker run cp-tsdata-demo
        │
        └─▶ Executes JAR with /app/cp-tsdata-geo-enrichment/data/in/
```

### Cloud Deployment (Inferred from release.sh)

```
ops/cloud-app-instances/
    └── cp-tsdata-demo-2/
        ├── released-apps/
        │   └── cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar
        ├── cfg/
        │   └── env.sh (APP_NAME, APP_VERSION)
        └── kst-context/
            └── schemas/
                └── *.avsc (Avro schemas)
```

**Deployment Process**:
1. Build JAR locally: `mvn clean package`
2. Run `bin/release.sh`
3. Copy JAR to `ops/cloud-app-instances/cp-tsdata-demo-2/released-apps/`
4. Build Docker image: `docker build . -t cp-tsdata-demo-2`
5. Push to registry (implied but not scripted)

---

## Observability

### Logging

**Current State**: Minimal

```java
System.out.println("> Read model files from: " + CSVFileRepository.repoPath);
System.out.println("[ITERATION] -> " + z);
System.out.println(sample);
```

**Limitations**:
- No structured logging
- No log levels (DEBUG, INFO, WARN, ERROR)
- No log aggregation

**Recommendations** (See IMPROVEMENTS.md):
- Use SLF4J with Logback
- Add structured logging (JSON format)
- Integrate with ELK stack or Datadog

### Metrics

**Current State**: None

**Recommendations**:
- JMX metrics for Kafka producers
- Custom metrics for simulation iterations
- Prometheus integration

### Tracing

**Current State**: None

**Recommendations**:
- Distributed tracing with Jaeger or Zipkin
- Correlate Kafka publish events with downstream processing

---

## Diagrams

### Component Diagram

```
┌────────────────────────────────────────────────────────────┐
│                  SimulationScenario                         │
│                   (Orchestrator)                            │
└──────┬─────────────────────────────────┬───────────────────┘
       │                                 │
       ▼                                 ▼
┌─────────────────┐            ┌──────────────────┐
│ CSVFileRepository│            │ GeoJSONExporter  │
│                 │            │                  │
│ - getPowerPlants│            │ - generateGrid   │
│ - getStations   │            │ - generateLinks  │
│ - getRegions    │            └──────────────────┘
└─────────────────┘
       │
       ▼
┌─────────────────┐
│   GridFactory   │
│                 │
│ - createNodes   │
│ - createLinks   │
└─────────────────┘
       │
       ▼
┌─────────────────────────────────────────────┐
│              TopicGroupTool                  │
│              (Kafka Facade)                  │
└──────┬──────────────────────────────┬───────┘
       │                              │
       ▼                              ▼
┌──────────────────┐        ┌──────────────────┐
│ RegionProducer   │        │ PowerSampleProd  │
│ StationProducer  │        │                  │
│ PlantProducer    │        │ - sendSample()   │
│ GridLinkProducer │        │ - flush()        │
└──────────────────┘        └──────────────────┘
       │                              │
       └──────────────┬───────────────┘
                      ▼
            ┌──────────────────┐
            │  Confluent Cloud │
            │   Kafka Cluster  │
            └──────────────────┘
```

---

## File Organization

```
src/main/java/
├── datamodel/
│   ├── graph_of_things/
│   │   ├── nodes/
│   │   │   ├── Node.java (abstract)
│   │   │   ├── PowerPlant.java
│   │   │   ├── Station.java
│   │   │   └── Region.java
│   │   └── relations/
│   │       ├── Relation.java (abstract)
│   │       ├── GridLink.java
│   │       └── InterNodeLink.java
│   ├── measurement/
│   │   └── PowerFlowSample.java
│   └── poi/
│       ├── POIData.java
│       └── POITypeEnum.java
├── dataprovider/
│   ├── CSVFileRepository.java
│   └── GridDataProvider.java
├── kafka/
│   ├── GenericProducerFactory.java
│   └── dataassets/
│       ├── TopicGroupTool.java
│       └── producers/
│           ├── RegionProducer.java
│           ├── StationProducer.java
│           ├── PlantProducer.java
│           ├── GridLinkProducer.java
│           └── PowerSampleProducer.java
└── tool/
    ├── SimulationScenario.java (main)
    ├── GridFactory.java
    └── geojson/
        └── GeoJSONExporter.java
```

---

## Summary

The **cp-tsdata-geo-enrichment** application follows a **layered architecture** with clear separation of concerns:

1. **Data Layer**: CSV file I/O with repository pattern
2. **Domain Layer**: Rich domain models representing power grid entities
3. **Integration Layer**: Kafka producers with topic-specific facades
4. **Presentation Layer**: Orchestration and simulation logic

**Strengths**:
- Modular design with clear boundaries
- Separation of context data (static) and streaming data (dynamic)
- Extensible domain model

**Areas for Improvement**:
- Introduce dependency injection
- Add interface abstractions for repositories and producers
- Implement comprehensive testing
- Enhance observability (logging, metrics, tracing)
- Externalize configuration

See `IMPROVEMENTS.md` for detailed enhancement recommendations.
