package datamodel.measurement;

import com.google.gson.Gson;
import tool.geojson.GeoJSONExporter;

/**
 * The power flow sample is a measurement obtained or simulated for a particular network link.
 */
public class PowerFlowSample {

    public static long t0 = System.currentTimeMillis();

    public String id = "";
    public long ts = 0;
    public double flow = 0.0;

    public PowerFlowSample(String id, int index, double power) {
        this.id = id;
        this.ts = t0 + 1000 * index;
        this.flow = power;
    }

    @Override
    public String toString() {
        return "PowerFlowSample{" +
                "id='" + id + '\'' +
                ", ts=" + ts +
                ", flow=" + flow +
                '}';
    }

    static Gson gson = new Gson();

    public String asJson() {
        return gson.toJson( this );
    }

}
