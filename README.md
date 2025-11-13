# cp-tsdata-geo-enrichment

> Geo-enrichment simulation for time series power grid data with Kafka streaming

A Java application that models electric power transmission networks, generates geospatial GeoJSON representations, and streams real-time power flow measurements to Apache Kafka/Confluent Cloud for downstream analytics and visualization.

## Quick Start

### Prerequisites

- Java 8 or higher
- Maven 3.x
- Access to Confluent Cloud (or local Kafka cluster)
- Docker (optional, for containerized deployment)

### Installation

1. **Clone the repository**

```bash
git clone https://github.com/kamir/cp-tsdata-geo-enrichment.git
cd cp-tsdata-geo-enrichment
```

2. **Configure Confluent Cloud credentials**

Copy the secure configuration template and add your credentials:

```bash
cp ccloud.props.secure ccloud.props
```

Edit `ccloud.props` and add your Kafka credentials:

```properties
bootstrap.servers=YOUR_CLUSTER.confluent.cloud:9092
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username='YOUR_API_KEY' password='YOUR_API_SECRET';
schema.registry.url=https://YOUR_SCHEMA_REGISTRY.confluent.cloud
schema.registry.basic.auth.user.info=SR_API_KEY:SR_API_SECRET
```

3. **Build the project**

```bash
mvn clean compile package install
```

This creates a fat JAR: `target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar`

### Running Locally

Execute the simulation with the provided sample data:

```bash
java -jar ./target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/
```

Or use the convenience script:

```bash
cd bin
./run_locally.sh
```

### Expected Output

The application will:

1. Load power grid data from CSV files in `./data/in/`
2. Generate GeoJSON files in `./data/out/`:
   - `nodes.geojson` - Power stations and plants as map points
   - `grid.json` - Station network topology
   - `links-result.json` - Regional connections
3. Publish context data to Kafka topics (regions, stations, plants, links)
4. Simulate 10 iterations of power flow measurements
5. Validate energy balance at each iteration

Example console output:

```
> Read model files from: ./data/in/
[ITERATION] -> 0
PowerFlowSample{linkId='L1', timestamp=1634567890, flowMW=450.23}
...
Export-Import : 1200.0 :: 1200.0 => 0.0
Prod-Cons     : 5000.0 :: 5000.0 => 0.0
Excess        : 0.0
[-------------------]

> Now we have to define the streams and tables in KSQLDB.
> Show GeoJSON data in browser: https://utahemre.github.io/geojsontest.html
```

## Project Structure

```
cp-tsdata-geo-enrichment/
├── src/main/java/
│   ├── datamodel/           # Domain models (Station, PowerPlant, Region, GridLink)
│   ├── dataprovider/        # CSV file I/O (CSVFileRepository, GridDataProvider)
│   ├── kafka/               # Kafka producers and topic management
│   │   ├── dataassets/      # Topic-specific producers
│   │   └── GenericProducerFactory.java
│   └── tool/                # Main application entry point and utilities
│       ├── SimulationScenario.java  # Main class
│       ├── GridFactory.java         # Factory for creating grid nodes
│       └── geojson/                 # GeoJSON export utilities
├── data/
│   ├── in/                  # Input CSV files (stations, plants, regions)
│   └── out/                 # Generated GeoJSON output
├── bin/                     # Execution scripts
├── ops/                     # Operational tools and deployment configs
├── docs/                    # Documentation
│   ├── PROJECT_OVERVIEW.md
│   ├── ARCHITECTURE.md
│   ├── DEVOPS.md
│   └── IMPROVEMENTS.md
├── pom.xml                  # Maven build configuration
├── Dockerfile               # Container image definition
└── ccloud.props             # Kafka/Confluent Cloud configuration
```

## Kafka Topics

The application publishes to these topics (prefix: `cp-tsdata.{appID}.`):

| Topic | Description | Type | Records |
|-------|-------------|------|---------|
| `grid-regions` | Regional boundaries, production/consumption capacity | Context | 3 |
| `grid-stations` | Transformer stations with geo-coordinates | Context | 9 |
| `grid-plants` | Power generation facilities | Context | 5 |
| `grid-links` | Transmission line connections | Context | ~15 |
| `grid-link-flow-data` | Time series power flow measurements | Streaming | Continuous |

**Note**: Default `appID` is `demo3` (see `SimulationScenario.java:23`)

## Data Model

### Input CSV Files

Place CSV files in `./data/in/`:

- **Sheet1** (Stations): `id, name, region, latitude, longitude`
- **Sheet2** (Power Plants): `id, name, stationId, capacityMW`
- **Sheet3** (Regions): `id, name, production, consumption, imports, exports`

### GeoJSON Output

Visualize the generated GeoJSON at: https://utahemre.github.io/geojsontest.html

1. Open the viewer
2. Copy contents of `./data/out/grid.json` or `links-result.json`
3. Paste into the viewer to see the power grid topology on a map

## Docker Deployment

Build and run the application in a container:

```bash
# Build the Docker image
docker build . -t cp-tsdata-demo

# Run the container
docker run --rm cp-tsdata-demo
```

**Note**: Ensure `ccloud.props.secure` contains valid credentials before building.

## Downstream Processing

After running the simulation, use ksqlDB to process the streams:

```sql
-- Create a stream from power flow measurements
CREATE STREAM power_flow_stream (
  linkId STRING,
  timestamp BIGINT,
  flowMW DOUBLE
) WITH (
  KAFKA_TOPIC='cp-tsdata.demo3.grid-link-flow-data',
  VALUE_FORMAT='JSON'
);

-- Calculate average flow per link over 5-minute windows
CREATE TABLE avg_flow_by_link AS
  SELECT linkId, AVG(flowMW) as avg_flow
  FROM power_flow_stream
  WINDOW TUMBLING (SIZE 5 MINUTES)
  GROUP BY linkId
  EMIT CHANGES;
```

## Configuration

### Application Configuration

- **appID**: Hardcoded in `SimulationScenario.java:23` (default: `demo3`)
- **Iterations**: Hardcoded to 10 in `SimulationScenario.java:92`
- **Data path**: Passed as command-line argument

### Kafka Configuration

All Kafka settings are in `ccloud.props`:

- Bootstrap servers
- SASL/SSL authentication
- Schema Registry URL and credentials

## Development

### Building from Source

```bash
mvn clean compile package install
```

### Running Tests

```bash
mvn test
```

**Note**: Test coverage is minimal. See `docs/IMPROVEMENTS.md` for recommended enhancements.

### Making Changes

1. Modify source code in `src/main/java/`
2. Rebuild: `mvn clean package`
3. Test locally: `java -jar target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar ./data/in/`
4. Commit changes to your feature branch

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to Kafka cluster

**Solution**: Verify `ccloud.props` credentials and network connectivity

```bash
# Test connectivity
curl -I https://YOUR_CLUSTER.confluent.cloud:9092
```

### Data Loading Errors

**Problem**: CSV files not found

**Solution**: Ensure data path argument points to directory containing CSV files:

```bash
java -jar target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar /absolute/path/to/data/in/
```

### Schema Registry Errors

**Problem**: Schema validation failures

**Solution**: Verify Schema Registry URL and credentials in `ccloud.props`

## Resources

- **Documentation**: See `docs/` directory for detailed architecture and operations guides
- **Presentation**: [Meetup Talk Slides](https://docs.google.com/presentation/d/1TqeDXCahjUrIr6aP1d_aSePNLC90fma1gL7CNLwVh9Y/)
- **PoC Documentation**: `docs/PowerGridSimulation-PoC3-KSQLDB-Application.pdf`
- **GeoJSON Viewer**: https://utahemre.github.io/geojsontest.html

## Contributing

This is a demonstration/proof-of-concept project. For improvements and suggestions, see `docs/IMPROVEMENTS.md`.

## License

See repository license information.

## Contact

For questions or issues, please refer to the project repository.
