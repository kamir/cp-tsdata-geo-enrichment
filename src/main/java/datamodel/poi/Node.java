package datamodel.poi;

import com.google.gson.Gson;
import tool.geojson.OrgJsonService;

import java.util.ArrayList;

public class Node {

    public POITypeEnum type = POITypeEnum.NODE;

    public String id = null;

    public String name = null;

    public double lat = 0.0;

    public double lon = 0.0;

    public String country = "";

    public String toGeoJSON() throws Exception {

        ArrayList<POIData> l = new ArrayList<>();
        l.add( this.asPoi() );

        return OrgJsonService.asGeoJson( l );

    }

    public POIData asPoi() {
        POIData p = new POIData( id, name, type, lon, lat);
        return p;
    }

    Gson gson = new Gson();

    public String asJson() {
        return gson.toJson( this );
    }

}
