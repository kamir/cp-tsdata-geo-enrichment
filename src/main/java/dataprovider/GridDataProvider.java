package dataprovider;

import java.util.Vector;

public class GridDataProvider {

    public static Vector<String> getSegments() {

        Vector<String> segments = new Vector<String>();

        /**
         * Define grid links between stations ...
         */
        segments.add( "1ST12345-2ST12345" );
        segments.add( "1ST12345-3ST12345" );
        segments.add( "2ST12345-3ST12345" );

        segments.add( "4ST12345-5ST12345" );
        segments.add( "6ST12345-5ST12345" );
        segments.add( "4ST12345-6ST12345" );

        segments.add( "7ST12345-9ST12345" );
        segments.add( "7ST12345-8ST12345" );
        segments.add( "8ST12345-9ST12345" );

        segments.add( "5ST12345-7ST12345" );
        segments.add( "6ST12345-7ST12345" );

        segments.add( "6ST12345-1ST12345" );
        segments.add( "6ST12345-2ST12345" );
        segments.add( "6ST12345-3ST12345" );

        segments.add( "7ST12345-1ST12345" );
        segments.add( "7ST12345-2ST12345" );
        segments.add( "7ST12345-3ST12345" );

        segments.add( "5ST12345-9ST12345" );

        return segments;

    }



}
