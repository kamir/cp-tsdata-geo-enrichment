package kafka.dataassets.producers;


import datamodel.graph_of_things.relations.GridLink;
import kafka.GenericProducerFactory;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class GridLinkProducer extends GenericProducerFactory {

    static String TOPIC = "grid-static-links";

    // public static Producer<String, GridLink> producer = null;

    public static void init( String appId ) {
        Properties props = GenericProducerFactory.getClientProperties();
        producer = createProducer( props, appId );
    }

    private static Producer<String, GridLink> createProducer(Properties props, String appId) {

        if( props.get( ProducerConfig.CLIENT_ID_CONFIG ) == null )
            props.put(ProducerConfig.CLIENT_ID_CONFIG, appId);

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");


        return new KafkaProducer<>(props);

    }

    public static void sendSample( GridLink s ) {

        long time = System.currentTimeMillis();

        try {

            RecordMetadata metadata = null;

            final ProducerRecord<String, GridLink> record =
                    new ProducerRecord<String, GridLink>(TOPIC, s.id, s );

            producer.send(record).get();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    };

}


