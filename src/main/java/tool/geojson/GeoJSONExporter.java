package tool.geojson;

import datamodel.graph_of_things.nodes.PowerPlant;
import datamodel.graph_of_things.nodes.Region;
import datamodel.graph_of_things.nodes.Station;
import datamodel.graph_of_things.relations.GridLink;
import tool.SimulationScenario;

import java.io.File;
import java.io.FileWriter;

public class GeoJSONExporter {

    static File out1 = new File( "/Users/mkampf/GITHUB.confluent/ENTSO-PoC/GITHUB/cp-tsdata-geo-enrichment/data/out/nodes.geojson" );
    static File out2 = new File( "/Users/mkampf/GITHUB.confluent/ENTSO-PoC/GITHUB/cp-tsdata-geo-enrichment/data/out/links-result.json" );
    static File out3 = new File( "/Users/mkampf/GITHUB.confluent/ENTSO-PoC/GITHUB/cp-tsdata-geo-enrichment/data/out/grid.json" );

    /*
    public static void generateNotes() throws Exception {
        FileWriter fw = new FileWriter( out1 );
        ArrayList allpois = new ArrayList();
        allpois.addAll( Scenario.getPPTAsPOI() );
        allpois.addAll( Scenario.getStationAsPOI() );

        fw.write( OrgJsonService.asGeoJson( allpois ) );
        fw.close();
    }
    */

    public static String generateRegionLinks() throws Exception {

        FileWriter fw = new FileWriter( out2 );

        StringBuffer sb = new StringBuffer( "{ \"type\": \"FeatureCollection\",\"features\": [ " );

        double delta = 0.02;
        double offset = 0.0;

        for(Region r1 : SimulationScenario.regions ) {
            for (Region r2 : SimulationScenario.regions) {
                if( r1.id != r2.id ) {
                     int a = Integer.parseInt( r1.id );
                     int b = Integer.parseInt( r2.id );

                     if( a > b ) {
                         offset = -1.0 * delta;
                     }
                     else
                         offset = 1.0 * delta;


                     System.out.println(RegionLink.getAsGeoJSON(r1, r2, offset));
                     sb.append( RegionLink.getAsGeoJSON(r1, r2, offset ) + "," ) ;

                }
            }
        }

        String s = sb.toString().substring(0,sb.toString().length()-1);
        s = s + " ] }";

        fw.write( s );
        fw.close();

        return s;

    }

    public static void generateGrid() throws Exception {

            FileWriter fw = new FileWriter( out3 );

            StringBuffer sb = new StringBuffer( "{ \"type\": \"FeatureCollection\",\"features\": [ " );

            double delta = 0.02;
            double offset = 0.0;

            for(Station s1 : SimulationScenario.stations ) {
                for (Station s2 : SimulationScenario.stations) {

                    if( SimulationScenario.segments.contains( s1.id+"-"+s2.id ) ) {

                        GridLink sl = new GridLink( s1, s2, 0, 100, 0.1, "STATIONLINK" );

                        String geojson = sl.getAsGeoJSON();
                        System.out.println( geojson );
                        sb.append( geojson + "," ) ;


                        SimulationScenario.collectStationLink( sl );

                    }
                }
                sb.append( s1.getGeoJSONStringForPOI() + ", ");
            }

            for(PowerPlant p : SimulationScenario.powerPlants ) {
                sb.append( p.getGeoJSONStringForPOI() + ", " );
            }

            String s = sb.toString().substring(0,sb.toString().length()-1);
            s = s + " ] }";

            fw.write( s );
            fw.close();

        }


}

class RegionLink {

    static String getAsGeoJSON( Region r1, Region r2, double offset ){

        return"{ \"type\": \"Feature\", \"geometry\": { \"type\": \"LineString\", \"coordinates\" : [ "+
                "[" + (r1.lon - offset) + "," + (r1.lat + offset) + "], [ " + (r2.lon - offset) + ", " + (r2.lat + offset) + "] "
                + " ] }, \"properties\": { \"popupContent\": \"Transfer: ("+r1.country+ "=>" + r2.country + ")" + SimulationScenario.getTotalFor(r1.id,r2.id) + "\" } }";

    }
}

