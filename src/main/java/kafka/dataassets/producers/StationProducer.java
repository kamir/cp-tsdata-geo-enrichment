package kafka.dataassets.producers;

import datamodel.graph_of_things.nodes.Station;
import kafka.GenericProducerFactory;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class StationProducer extends GenericProducerFactory {

    static Properties props;

    static String TOPIC = "grid-stations";

    public static void init( String appId ) {
        Properties props = GenericProducerFactory.getClientProperties();
        producer = createProducer( props, appId );
    }

    public static void sendSample( Station s ) {

        long time = System.currentTimeMillis();

        try {

            RecordMetadata metadata = null;

            final ProducerRecord<String, Station> record =
                    new ProducerRecord<String,Station>(TOPIC, s.id, s );

            producer.send(record).get();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static Producer<String, Station> createProducer(Properties props, String appId) {

        if( props.get( ProducerConfig.CLIENT_ID_CONFIG ) == null )
            props.put(ProducerConfig.CLIENT_ID_CONFIG, appId);

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<>(props);

    }


}


