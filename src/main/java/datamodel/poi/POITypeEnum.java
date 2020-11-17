package datamodel.poi;

public enum POITypeEnum {

    NODE("NODE"),
    R("REGION"),
    PPT("POWERPLANT"),
    STATION("STATION"),
    BM("BORDERMARKER");

    public String getLabel() {
        return label;
    }

    public String getImageFile() {
        return name().toLowerCase() + ".png";
    }

    private final String label;

    private POITypeEnum(String label) {
        this.label = label;
    }

    public static POITypeEnum ofImage(String image) {
        return valueOf(image.replace(".png", "").toUpperCase());
    }
}