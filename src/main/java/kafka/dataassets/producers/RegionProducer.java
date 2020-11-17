package kafka.dataassets.producers;

import datamodel.graph_of_things.nodes.Region;
import kafka.GenericProducerFactory;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class RegionProducer extends GenericProducerFactory {

    static Properties props;

    static String TOPIC = "grid-regions";

    // static Producer<String, Region> producer = null;

    public static void init( String appId ) {
        Properties props = GenericProducerFactory.getClientProperties();
        producer = createProducer( props, appId );
    }

    public static void sendSample( Region r ) {

        try {

            RecordMetadata metadata = null;

            final ProducerRecord<String, Region> record =
                    new ProducerRecord<String,Region>(TOPIC, r.id, r );

            producer.send(record).get();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static Producer<String, Region> createProducer(Properties props, String appID) {

        if( props.get( ProducerConfig.CLIENT_ID_CONFIG ) == null )
            props.put(ProducerConfig.CLIENT_ID_CONFIG, appID);

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<>(props);

    }



}


