package datamodel.graph_of_things.relations;

import datamodel.poi.Node;
import datamodel.measurement.PowerFlowSample;

public class GridLink extends InterNodeLink {

    /**
     * Relation specific properties:
     */
    public double avgFlow = 0.0;
    public double epsilon = 0.0;

    public String id = null;

    public String regionContextTag = null;

    public GridLink(Node r1, Node r2, double visualOffsetOnMap4LatLon, double avgFlow, double epsilon, String linkType ) {

        super(visualOffsetOnMap4LatLon, r1, r2, linkType);
        this.avgFlow = avgFlow;
        this.epsilon = epsilon;

        // source => target
        this.id = this.r1.id+"-"+this.r2.id;

        // this is the explicit information about the regions, a station is in ...
        this.regionContextTag = this.r1.country + "->" + this.r2.country;

    }

    /**
     * Generate a link specific property for a simulation run.
     *
     * @param index
     * @return
     */
    public PowerFlowSample newSample(int index) {


        double power = avgFlow;

        double band = avgFlow * epsilon;
        double delta = Math.random() * band;

        // white noise - no memory
        if ( Math.random() > 0.5 )
           power = power + delta;
        else
           power = power - delta;

        return new PowerFlowSample( id, index, power );

    }

}
