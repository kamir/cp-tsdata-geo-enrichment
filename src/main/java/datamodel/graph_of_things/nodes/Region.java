package datamodel.graph_of_things.nodes;

import datamodel.poi.Node;
import datamodel.poi.POIData;
import datamodel.poi.POITypeEnum;

public class Region extends Node {

    public double production = 0;
    public double consumption = 0;
    public double imports = 0;
    public double exports = 0;

    public Region(String[] FIELDS) {

        super();
        super.type = POITypeEnum.R;
        super.id = FIELDS[0];
        super.country = FIELDS[1];
        super.name = FIELDS[2];
        super.lat = Double.parseDouble( FIELDS[3] );
        super.lon = Double.parseDouble( FIELDS[4] );
        production = Double.parseDouble( FIELDS[5] );
        consumption = Double.parseDouble( FIELDS[6] );
        imports = Double.parseDouble( FIELDS[7] );
        exports = Double.parseDouble( FIELDS[8] );

    };

    @Override
    public String toString() {
        return "Region{" +
                "type=" + type +
                ", production=" + production +
                ", consumption=" + consumption +
                ", imports=" + imports +
                ", exports=" + exports +
                ", type=" + type +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", country='" + country + '\'' +
                '}';
    }

    @Override
    public POIData asPoi() {
        POIData p = new POIData( id, name, type, lon, lat);
        p.addProperty( "production", production +"");
        p.addProperty( "consumption", consumption +"");
        p.addProperty( "imports", imports +"");
        p.addProperty( "exports", exports +"");
        return p;
    }

}
