package tool;

import datamodel.measurement.PowerFlowSample;
import datamodel.poi.POIData;
import datamodel.graph_of_things.nodes.Region;
import datamodel.graph_of_things.nodes.PowerPlant;
import datamodel.graph_of_things.nodes.Station;
import datamodel.graph_of_things.relations.GridLink;
import dataprovider.CSVFileRepository;
import dataprovider.GridDataProvider;
import kafka.dataassets.TopicGroupTool;
import kafka.dataassets.producers.*;
import tool.geojson.GeoJSONExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;


public class SimulationScenario {

    static public String appID = "demo3";

    /**
     * Data from CSV files.
     */
    static public Vector<PowerPlant> powerPlants  = new Vector<PowerPlant>();
    static public Vector<Station>    stations     = new Vector<Station>();
    static public Vector<Region>     regions      = new Vector<Region>();

    /**
     * Static data defined for the demo
     */
    static public Vector<String>     segments     = new Vector<String>();


    /**
     * Derived data - calculated for the analysis process.
     */
    static public Vector<GridLink>   gridLinks     = new Vector<GridLink>();

    /**
     * GeoTools Quickstart demo application. Prompts the user for a shapefile and displays its
     * contents on the screen in a map frame
     */
    public static void main(String[] args) throws Exception {

        powerPlants = CSVFileRepository.getPowerPlantsFromRepository();
        stations = CSVFileRepository.getStationsFromRepository();
        regions = CSVFileRepository.getRegionsFromRepository();
        segments = GridDataProvider.getSegments();

        /**
         *   This is the network layer which represents the reality (simulation setup).
         */
        GeoJSONExporter.generateGrid();

        /**
         *   This is the network layer which represents the analysis results.
         */
        GeoJSONExporter.generateRegionLinks();

        /**
         *   Provide data via topics in Confluent cloud.
         */
        TopicGroupTool.configureProducer(appID);

        TopicGroupTool.storeRegionContextData( regions );
        TopicGroupTool.storeStationContextData( stations );
        TopicGroupTool.storePlantContextData( powerPlants );
        TopicGroupTool.storeLinkContextData( gridLinks );

        System.out.println( "> Now we have to define the streams and tables in KSQLDB. " );

        simulateFlow();

        System.out.println( "> Show GeoJSON data in browser: https://utahemre.github.io/geojsontest.html " );

    }


    private static void simulateFlow() {

        int z = 0;

        while ( z < 10 ) {

            System.out.println( "[ITERATION] -> " + z);
            for (GridLink sl : gridLinks) {

                PowerFlowSample sample = sl.newSample(z);

                System.out.println(sample);

                PowerSampleProducer.sendSample( sample );

            }
            PowerSampleProducer.flush();
            z++;
            System.out.println( "[-------------------]");
            System.out.println( "");

            /**
             * This is a validation of the static flows, predefined in our setup.
             */
            calcBalanceForRegion( regions );

        }

    }

    private static void calcBalanceForRegion(Vector<Region> regions) {
        double totalProd = 0;
        double totalCons = 0;
        double totalExp = 0;
        double totalImp = 0;
        for( Region r : regions ) {
            totalProd = totalProd + r.production;
            totalCons = totalCons + r.consumption;
            totalExp  = totalExp + r.exports;
            totalImp  = totalImp + r.imports;
        }
        System.out.println( "Export-Import : " + totalExp + " :: " + totalImp + " => " + ( totalExp - totalImp ) );
        System.out.println( "Prod-Cons     : " + totalProd + " :: " + totalCons + " => " + ( totalProd - totalCons ) );
        System.out.println( "Excess        : " + ( ( totalProd - totalCons )-( totalExp - totalImp ) ) );
        System.out.println();
    }

    public static List<POIData> getPPTAsPOI() {

        ArrayList<POIData> l = new ArrayList<>();
        for( PowerPlant s : powerPlants ) {

            l.add(s.asPoi());

        }

        return l ;

    }

    public static List<POIData> getStationAsPOI() {

        ArrayList<POIData> l = new ArrayList<>();
        for( Station s : stations ) {

            l.add(s.asPoi());

        }

        return l ;

    }


    public static Collection getRegionsAsPOI() {

        ArrayList<POIData> l = new ArrayList<>();
        for( Region s : regions ) {

            l.add(s.asPoi());

        }

        return l ;
    }

    public static String getTotalFor(String id, String id1) {
        return "42";
    }

    public static Station getStationWithID(String linkedToStation) {
        for( Station s : stations ) {
            if ( s.id.equals( linkedToStation ) ) {
                return s;
            }
        }
        return null;
    }

    public static void collectStationLink(GridLink sl) {
        gridLinks.add( sl );
    }
}
