package kafka.dataassets.producers;

import datamodel.graph_of_things.nodes.PowerPlant;
import kafka.GenericProducerFactory;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class PlantProducer extends GenericProducerFactory {

    static Properties props;

    static String TOPIC = "grid-plants";

    // static Producer<String, PowerPlant> producer = null;

    public static void init( String appId ) {
        Properties props = GenericProducerFactory.getClientProperties();
        producer = createProducer( props, appId );
    }

    public static void sendSample( PowerPlant p ) {

        long time = System.currentTimeMillis();

        try {

            RecordMetadata metadata = null;

            final ProducerRecord<String, PowerPlant> record =
                    new ProducerRecord<String,PowerPlant>(TOPIC, p.id, p );

            producer.send(record).get();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static Producer<String, PowerPlant> createProducer(Properties props, String appID) {

        if( props.get( ProducerConfig.CLIENT_ID_CONFIG ) == null )
            props.put(ProducerConfig.CLIENT_ID_CONFIG, appID);

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<>(props);

    }

}


