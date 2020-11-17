package datamodel.graph_of_things.nodes;

import datamodel.poi.Node;
import datamodel.poi.POIData;
import datamodel.poi.POITypeEnum;
import datamodel.graph_of_things.relations.GridLink;
import tool.geojson.OrgJsonService;
import tool.SimulationScenario;

public class PowerPlant extends Node {

    public PowerPlant(String[] FIELDS) {

        super();
        super.type = POITypeEnum.PPT;
        super.id = FIELDS[0];
        super.country = FIELDS[1];
        super.name = FIELDS[2];
        super.lat = Double.parseDouble( FIELDS[3] );
        super.lon = Double.parseDouble( FIELDS[4] );
        production = Double.parseDouble( FIELDS[5] );

        linkedToStation = FIELDS[6];

    };

    public double production = 1.0; // MW

    public String linkedToStation = "?";

    @Override
    public String toString() {
        return "PowerPlant{" +
                "country='" + country + '\'' +
                ", production=" + production +
                ", linkedToStation=" + linkedToStation +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }

    @Override
    public POIData asPoi() {
        POIData p = new POIData( id, name, type, lon, lat);
        p.addProperty( "production", production +"");
        return p;
    }

    public String getGeoJSONStringForPOI() throws Exception {
        return OrgJsonService.asGeoJson( this.asPoi() ) + ", " + getGeoJSONStringForStationLink();
    }

    public String getGeoJSONStringForStationLink() throws Exception {

        GridLink sl = new GridLink( this, SimulationScenario.getStationWithID(linkedToStation), 0, production, 0.1, "STATIONLINK") ;
        SimulationScenario.collectStationLink( sl );

        String l = sl.getAsGeoJSON();
        return l;

    }


}
