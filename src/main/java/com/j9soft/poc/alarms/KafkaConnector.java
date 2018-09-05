package com.j9soft.poc.alarms;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;


/**
 * Provider of producer and consumer connected to a Kafka cluster.
 */
public class KafkaConnector {

    private static final String TOPIC_NAME_RAW_ACTIVE_ALARMS = "tc_raw_active_alarms";

    private static final Logger logger = LoggerFactory.getLogger(KafkaConnector.class);

    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;

    void connect(final String brokerHost, final Integer brokerPort) {

        // Producer:
        //
        // Producer configuration.
        Properties producerProps = new Properties();
        producerProps.setProperty("bootstrap.servers", brokerHost + ":" + brokerPort);
        producerProps.setProperty("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        //
        // Create a new instance.
        producer = new KafkaProducer<>(producerProps);
        logger.info("Kafka producer: " + producer);

        // Consumer:
        //
        // Consumer configuration.
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", brokerHost + ":" + brokerPort);
        // We use different group IDs because according to KafkaConsumer javadoc:
        //   "To avoid offset commit conflicts, you should usually ensure that the groupId is unique for each consumer instance. "
        consumerProps.setProperty("group.id", "raasV2-" + UUID.randomUUID());
        consumerProps.setProperty("client.id", "consumer0");
        consumerProps.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");  // to make sure the consumer starts from the beginning of the topic
        //
        // Create a new instance.
        consumer = new KafkaConsumer<>(consumerProps);
        logger.info("Kafka consumer: " + consumer);
    }

    public KafkaConsumer<String,byte[]> getConsumer() {
        return this.consumer;
    }
    public KafkaProducer<String, byte[]> getProducer() {
        return this.producer;
    }

    public String getTopicName() {
        return TOPIC_NAME_RAW_ACTIVE_ALARMS;
    }

    void close() {
        producer.close();
        consumer.close();
    }

}
