package datamodel.graph_of_things.nodes;

import datamodel.poi.Node;
import datamodel.poi.POIData;
import datamodel.poi.POITypeEnum;
import tool.geojson.OrgJsonService;

public class Station extends Node {

    public Station(String[] FIELDS) {
        super();
        super.type = POITypeEnum.STATION;
        super.id = FIELDS[0];
        country = FIELDS[1];
        super.name = FIELDS[2];
        super.lat = Double.parseDouble( FIELDS[3] );
        super.lon = Double.parseDouble( FIELDS[4] );

    }

    @Override
    public POIData asPoi() {
        POIData p = new POIData( id, name, type, lon, lat);
        p.addProperty( "popupContent", "id="+ this.id + ", name=" + this.name );
        return p;
    }


    @Override
    public String toString() {
        return "Station{" +
                "id='" + id + '\'' +
                ", city='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }

    public String getGeoJSONStringForPOI() throws Exception {
        return OrgJsonService.asGeoJson( this.asPoi() );
    }

}
