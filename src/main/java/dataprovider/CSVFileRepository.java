package dataprovider;

import datamodel.graph_of_things.nodes.PowerPlant;
import datamodel.graph_of_things.nodes.Region;
import datamodel.graph_of_things.nodes.Station;
import tool.GridFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class CSVFileRepository {

    static String repoPath = "/Users/mkampf/GITHUB.confluent/ENTSO-PoC/GITHUB/cp-tsdata-geo-enrichment/data/in/";

    public static Vector<PowerPlant> getPowerPlantsFromRepository() throws IOException {

        Vector<PowerPlant> powerPlants = new Vector<>();

        File f = new File( repoPath + "ENTSO-E-Grid - Sheet2.csv" );
        System.out.println( f.getAbsolutePath() + " => " + f.exists() );

        /**
         *   Load PowerPlants
         */
        FileReader fr = new FileReader(f);

        BufferedReader br = new BufferedReader( fr );

        int i = 0;
        while( br.ready() ) {

            String line = br.readLine();
            if ( i > 0 ) {

                PowerPlant pp = GridFactory.getGetPPNode( line.split(",") );
                powerPlants.add( pp );

                System.out.println( pp.toString() );
            }
            i++;

        }

        return powerPlants;
    }

    public static Vector<Station> getStationsFromRepository() throws IOException {

        Vector<Station> stations = new Vector<>();

        /**
         *   Load Stations
         */
        FileReader fr2 = new FileReader(repoPath + "ENTSO-E-Grid - Sheet1.csv" );

        BufferedReader br2 = new BufferedReader( fr2 );

        int j = 0;
        while( br2.ready() ) {

            String line = br2.readLine();

            if( j > 0 ) {
                Station s = GridFactory.getGetSNode(line.split(","));
                System.out.println(s.toString());
                stations.add( s );
            }
            j++;
        }

        return stations;
    }

    public static Vector<Region> getRegionsFromRepository() throws IOException {

        Vector<Region> regions = new Vector<>();

        /**
         *   Load regions
         */
        FileReader fr3 = new FileReader( repoPath + "ENTSO-E-Grid - Sheet3.csv" );
        FileReader fr31 = new FileReader(repoPath + "ENTSO-E-Grid - Sheet3-1.csv" );
        FileReader fr32 = new FileReader(repoPath + "ENTSO-E-Grid - Sheet3-2.csv" );

        BufferedReader br3 = new BufferedReader( fr3 );

        int k = 0;
        while( br3.ready() ) {

            String line = br3.readLine();
            if ( k > 0 ) {

                Region r = GridFactory.getGetRegionNode( line.split(",") );
                regions.add( r );

                System.out.println( r.toString() );
            }
            k++;

        }

        return regions;

    }

}
