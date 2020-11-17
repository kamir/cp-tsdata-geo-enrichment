package datamodel.graph_of_things.relations;

import datamodel.poi.Node;

public class InterNodeLink {
    double visualOffsetOnMap4LatLon = 0.0;
    Node r1 = null;
    Node r2 = null;
    String linkType = "?";

    public InterNodeLink(double visualOffsetOnMap4LatLon, Node r1, Node r2, String linkType) {
        this.visualOffsetOnMap4LatLon = visualOffsetOnMap4LatLon;
        this.r1 = r1;
        this.r2 = r2;
        this.linkType = linkType;
    }

    public String getAsGeoJSON(){

        return"{ \"type\": \"Feature\", \"geometry\": { \"type\": \"LineString\", \"coordinates\" : [ "+
                "[" + (r1.lon - visualOffsetOnMap4LatLon) + "," + (r1.lat + visualOffsetOnMap4LatLon) + "], [ " + (r2.lon - visualOffsetOnMap4LatLon) + ", " + (r2.lat + visualOffsetOnMap4LatLon) + "] "
                + " ] }, \"properties\": { \"popupContent\": \""+linkType+": ("+r1.name+ "=>" + r2.id + ")" + "\" } }";

    }
}
