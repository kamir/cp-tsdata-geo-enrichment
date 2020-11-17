package tool.geojson;

import datamodel.poi.POIData;
import datamodel.poi.POITypeEnum;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OrgJsonService {

    public static String asGeoJson(POIData poi) throws Exception {
            JSONArray coordinatesJson = new JSONArray();
            coordinatesJson.put(poi.getLongitude());
            coordinatesJson.put(poi.getLatitude());

            JSONObject geometryJson = new JSONObject();
            geometryJson.put("type", "Point");
            geometryJson.put("coordinates", coordinatesJson);

            JSONObject propertiesJson = new JSONObject();
            propertiesJson.put("name", poi.getName());
            propertiesJson.put("image", poi.getType().getImageFile());

            for( String k : poi.getProperties().stringPropertyNames() ) {
                propertiesJson.put( k, poi.getProperties().get( k ) );
            }
            JSONObject featureJson = new JSONObject();
            featureJson.put("type", "Feature");
            featureJson.put("id", poi.getId());
            featureJson.put("properties", propertiesJson);
            featureJson.put("geometry", geometryJson);

        return featureJson.toString();
    }


    public static String asGeoJson(List<POIData> pois) throws Exception {
        JSONArray featuresJson = new JSONArray();
        for (POIData poi : pois) {
            JSONArray coordinatesJson = new JSONArray();
            coordinatesJson.put(poi.getLongitude());
            coordinatesJson.put(poi.getLatitude());

            JSONObject geometryJson = new JSONObject();
            geometryJson.put("type", "Point");
            geometryJson.put("coordinates", coordinatesJson);

            JSONObject propertiesJson = new JSONObject();
            propertiesJson.put("name", poi.getName());
            propertiesJson.put("image", poi.getType().getImageFile());

            for( String k : poi.getProperties().stringPropertyNames() ) {
                propertiesJson.put( k, poi.getProperties().get( k ) );
            }
            JSONObject featureJson = new JSONObject();
            featureJson.put("type", "Feature");
            featureJson.put("id", poi.getId());
            featureJson.put("properties", propertiesJson);
            featureJson.put("geometry", geometryJson);
            featuresJson.put(featureJson);
        }
        JSONObject featureCollectionJson = new JSONObject();
        featureCollectionJson.put("type", "FeatureCollection");
        featureCollectionJson.put("features", featuresJson);

        return featureCollectionJson.toString();
    }

    public List<POIData> asPois(String geoJson) throws Exception {
        List<POIData> pois = new ArrayList<POIData>();
        JSONObject jsonRoot = new JSONObject(geoJson);
        JSONArray features = jsonRoot.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            long id = feature.getLong("id");

            JSONObject properties = (JSONObject) feature.get("properties");
            String name = properties.getString("name");
            POITypeEnum type = POITypeEnum.ofImage(properties.getString("image"));

            JSONObject geometry = (JSONObject) feature.get("geometry");
            JSONArray coordinates = (JSONArray) geometry.get("coordinates");
            double longitude = coordinates.getDouble(0);
            double latitude = coordinates.getDouble(1);

            POIData poiData = new POIData(id+"", name, type, longitude, latitude);
            pois.add(poiData);
        }
        return pois;
    }
}