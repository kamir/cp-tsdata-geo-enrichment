# Project Overview: cp-tsdata-geo-enrichment

## Executive Summary

**cp-tsdata-geo-enrichment** is a geo-enrichment simulation application for time series power grid data. It models electric power transmission networks, generates geospatial representations, and streams real-time power flow measurements to Apache Kafka/Confluent Cloud for downstream stream processing and analytics.

## Purpose and Business Value

The project serves as a **data generator and simulation platform** for demonstrating:

- **Real-time stream processing** with Kafka and ksqlDB
- **Geo-enrichment** of time series data with geographic context
- **Power grid topology modeling** with stations, plants, and regional boundaries
- **Event-driven architecture** patterns for energy sector applications
- **Graph-based network analysis** for infrastructure monitoring

### Key Use Cases

1. **Energy Grid Monitoring** - Simulate real-time power flow across transmission networks
2. **Stream Processing Demonstrations** - Provide realistic data streams for Confluent Platform demos
3. **Geospatial Analytics** - Enable map-based visualization of grid topology and power flows
4. **ksqlDB Application Development** - Test stream processing queries against realistic power grid data
5. **Educational/Meetup Presentations** - Demonstrate complex event processing with domain-specific data

## Technical Overview

### What It Does

The application:

1. **Loads** power grid configuration from CSV files (stations, plants, regions)
2. **Models** network topology as a graph of interconnected nodes
3. **Generates** GeoJSON representations for web-based map visualization
4. **Simulates** power flow measurements with realistic variability (white noise)
5. **Publishes** both context data and streaming measurements to Kafka topics
6. **Validates** energy balance constraints (production vs. consumption, imports vs. exports)

### Data Flow

```
CSV Files → Java Data Models → GeoJSON Export → Kafka Topics
   ↓                ↓                              ↓
Stations       Grid Graph                  Stream Processing
Plants         (JUNG-based)                 (ksqlDB Consumers)
Regions        Topology                     Analytics
```

### Kafka Topic Schema

The application publishes to 5 Kafka topics with the pattern `cp-tsdata.{appID}.{entity}`:

| Topic | Purpose | Record Type | Update Frequency |
|-------|---------|-------------|------------------|
| `grid-regions` | Regional boundaries and production/consumption capacity | Context (Static) | Once at startup |
| `grid-stations` | Transformer station locations and metadata | Context (Static) | Once at startup |
| `grid-plants` | Power plant locations and generation capacity | Context (Static) | Once at startup |
| `grid-links` | Transmission line connections between stations | Context (Static) | Once at startup |
| `grid-link-flow-data` | Time series power flow measurements | Streaming (Dynamic) | Continuous (10 iterations per run) |

### Simulation Model

The simulation uses a **simplified power flow model**:

- Each `GridLink` has a nominal flow rate (MW) and variance (epsilon)
- Flow samples are generated as: `nominalFlow ± random(epsilon)`
- Energy balance is validated at each iteration:
  - `Production - Consumption = Exports - Imports`
  - Regional excess capacity is tracked
  - Imbalances indicate grid stress or configuration errors

### Technology Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Language | Java | 8 |
| Build | Maven | 3.x |
| Message Broker | Apache Kafka | 2.6.0 |
| Confluent Platform | Confluent Cloud | 6.0.0 |
| Serialization | JSON, Avro | 1.10.0 |
| Schema Registry | Confluent Schema Registry | 6.0.0 |
| JSON Processing | Gson, org.json | 2.7 |
| Graph Library | JUNG (commented out) | 2.0.1 |
| Containerization | Docker | - |
| Testing | JUnit | 4.12 |

## Current State

### Data Model

The project models a **European power grid** with:

- **3 Regions**: Belgium, Germany, France
- **9 Transformer Stations**: Distributed across regions
- **5 Power Plants**: Generation facilities with capacity ratings
- **Multiple Grid Links**: Transmission lines connecting stations

### Sample Data

Located in `/data/in/`:

- `E-Grid - Sheet1.csv` - Station definitions (id, name, region, coordinates)
- `E-Grid - Sheet2.csv` - Power plant definitions (id, name, station, capacity)
- `E-Grid - Sheet3.csv` - Regional definitions (id, name, production, consumption, imports, exports)

### Output Artifacts

Located in `/data/out/`:

- `nodes.geojson` - All nodes (stations, plants) as GeoJSON point features
- `grid.json` - Station network grid as GeoJSON FeatureCollection
- `links-result.json` - Regional connections as GeoJSON FeatureCollection

These can be visualized at: https://utahemre.github.io/geojsontest.html

## Project Maturity

**Status**: Proof of Concept / Demo Application

**Strengths**:
- ✅ Working Kafka integration
- ✅ GeoJSON export for visualization
- ✅ Modular code structure
- ✅ Docker containerization
- ✅ Energy balance validation

**Limitations**:
- ⚠️ No automated tests
- ⚠️ Hardcoded simulation parameters
- ⚠️ Limited error handling
- ⚠️ No configuration management
- ⚠️ Minimal logging/observability
- ⚠️ CSV data schema not validated
- ⚠️ Single-threaded execution

## Related Resources

- **Presentation**: [Google Slides Deck](https://docs.google.com/presentation/d/1TqeDXCahjUrIr6aP1d_aSePNLC90fma1gL7CNLwVh9Y/)
- **Documentation**: `docs/PowerGridSimulation-PoC3-KSQLDB-Application.pdf`
- **GeoJSON Viewer**: https://utahemre.github.io/geojsontest.html
- **GitHub Repository**: https://github.com/kamir/cp-tsdata-geo-enrichment

## Development Branch

Current feature branch: `claude/create-project-documentation-011CV5V3X2n8Qg1d3pG9NE6o`

Main repository: https://github.com/kamir/cp-tsdata-geo-enrichment

## Next Steps

See `IMPROVEMENTS.md` for recommended enhancements and roadmap items.
