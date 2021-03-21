package kafka.dataassets.producers;

import datamodel.measurement.PowerFlowSample;

import kafka.GenericProducerFactory;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;

public class PowerSampleProducer extends GenericProducerFactory {

    static Properties props;

    static String TOPIC = namespace + "grid-link-flow-data";

    // static Producer<String, PowerFlowSample> producer = null;

    public static void init( String appId ) {
        Properties props = GenericProducerFactory.getClientProperties();
        producer = createProducer( props, appId );
    }

    public static void sendSample( PowerFlowSample sample ) {


        long time = System.currentTimeMillis();

        try {

            RecordMetadata metadata = null;

            final ProducerRecord<String, PowerFlowSample> record =
                    new ProducerRecord<String,PowerFlowSample>(TOPIC, sample.id, sample );

            producer.send(record).get();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static Producer<String, PowerFlowSample> createProducer(Properties props, String appId) {

        if( props.get( ProducerConfig.CLIENT_ID_CONFIG ) == null )
            props.put(ProducerConfig.CLIENT_ID_CONFIG, appId);


        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");


        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");


        return new KafkaProducer<>(props);

    }




}
