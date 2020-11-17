package datamodel.poi;

import java.util.Properties;

public class POIData {

    private String id;
    private String name;
    private POITypeEnum type;
    private double latitude;
    private double longitude;

    public POIData(String id, String name, POITypeEnum type, double longitude, double latitude) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public POITypeEnum getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    Properties props = new Properties();

    public void addProperty( String k, String v ) {
        props.put( k, v );
    }

    public Properties getProperties() {
        return props;
    }
}