package tool;

import datamodel.graph_of_things.nodes.PowerPlant;
import datamodel.graph_of_things.nodes.Region;
import datamodel.graph_of_things.nodes.Station;

public class GridFactory {

    /**
     * Create a PowerPlan instance from CSV record
     * @param FILEDS
     * @return
     */
    public static PowerPlant getGetPPNode(String[] FILEDS ) {

        PowerPlant pp = new PowerPlant( FILEDS );
        return pp;

    }

    /**
     * Create a Station instance from CSV record
     * @param FIELDS
     * @return
     */
    public static Station getGetSNode(String[] FIELDS) {

        Station st = new Station(FIELDS);
        return st;

    }

    /**
     * Create a Region instance from CSV record
     * @param FIELDS
     * @return
     */
    public static Region getGetRegionNode(String[] FIELDS) {

        Region r = new Region( FIELDS );
        return r;
    }

}
