package kafka.dataassets;

import datamodel.graph_of_things.nodes.PowerPlant;
import datamodel.graph_of_things.nodes.Region;
import datamodel.graph_of_things.nodes.Station;
import datamodel.graph_of_things.relations.GridLink;
import kafka.dataassets.producers.*;

import java.util.Vector;

public class TopicGroupTool {

    public static void configureProducer( String appID ) {

        PowerSampleProducer.init( appID );
        RegionProducer.init( appID );
        PlantProducer.init( appID );
        StationProducer.init( appID );
        GridLinkProducer.init( appID );

    }


    public static void storeLinkContextData(Vector<GridLink> gridLinks) {
        for( GridLink link : gridLinks ) {
            GridLinkProducer.sendSample( link );
        }
    }

    public static void storeStationContextData(Vector<Station> stations) {
        for( Station s : stations ) {
            StationProducer.sendSample( s );
        }
    }

    public static void storeRegionContextData(Vector<Region> regions) {
        for( Region r : regions ) {
            RegionProducer.sendSample( r );
        }
    }

    public static void storePlantContextData(Vector<PowerPlant> powerPlants) {
        for( PowerPlant p : powerPlants ) {
            PlantProducer.sendSample( p );
        }
    }

}
