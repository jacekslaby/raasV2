package com.j9soft.poc.alarms;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of data access layer (DAO) based on a Kafka producer.
 *
 * This Dao is used in production mode, i.e. in production environments.
 */
@Service
public class RaasDaoKafka implements RaasDao {

    private static final Logger logger = LoggerFactory.getLogger(RaasDaoKafka.class);

    private String topicName;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;

    /**
     * Note: Autowire - The idea is that it is possible to create a new class annotated as @Configuration
     *  and this class will get autowired here. (btw: In this class it is important remember about a destroy method to close a producer.)
     */
    @Autowired
    RaasDaoKafka(KafkaConnector connector) {
        this.topicName = connector.getTopicName();
        this.producer = connector.getProducer();
        this.consumer = connector.getConsumer();
    }

    @Override
    public void createOrUpdateAlarm(String domain, String adapterName, String notificationIdentifier, String value) {

        ProducerRecord<String, byte[]> data = new ProducerRecord<>(
                topicName, notificationIdentifier, value.getBytes(StandardCharsets.UTF_8));

        logger.info("createOrUpdateAlarm: '{}' - start", notificationIdentifier);
        try {
            // @TODO save to a subpartition assigned to this adapter

            producer.send(data).get(); // We want to save it immediately.

        } catch (InterruptedException|ExecutionException e) {
            logger.info("createOrUpdateAlarm: '{}' - failure", notificationIdentifier);
            throw new RuntimeException("Failed to save the alarm: " + notificationIdentifier, e);  // @TODO add exception to API
        }

        logger.info("createOrUpdateAlarm: '{}' - success", notificationIdentifier);
    }

    @Override
    public RawAlarmsPack queryAlarms(String domain, String adapterName, String subpartitionName,
                                     String tagOfTheFirstAlarmToBeReturned, int howMany) {

        logger.info("queryAlarms: subpartitionName='{}' - start", subpartitionName);

        RawAlarmsPack result = new RawAlarmsPack();

        // Calculate current end of data in this kafka partition.
        //
        TopicPartition partition = new TopicPartition(topicName, Integer.parseInt(subpartitionName));
        Collection<TopicPartition> partitions = Arrays.asList(partition);
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
        long subpartitionEndOffset =  endOffsets.get(partition);

        // Rewind to the beginning of data to be retrieved from this kafka partition.
        //
        consumer.assign(partitions);  // btw: We do not use subscribe(), i.e. we do not use kafka's built-in group coordination.
        if (tagOfTheFirstAlarmToBeReturned != null) {
            logger.info("queryAlarms: subpartitionName='{}' - seek:", tagOfTheFirstAlarmToBeReturned);
            consumer.seek(partition, Long.parseLong(tagOfTheFirstAlarmToBeReturned)); // We want to load another pack.
        } else {
            logger.info("queryAlarms: subpartitionName='{}' - seekToBeginning", subpartitionName);
            consumer.seekToBeginning(partitions);  // We want to load all contents.
        }

        // Read the data.
        //
        // @TODO remove duplicated keys (i.e. compaction), remove keys with null value
        //
        List<String> notificationIdentifiers = new ArrayList<>(howMany);
        List<String> values = new ArrayList<>(howMany);
        ConsumerRecords<String, byte[]> records;
        do {
            records = consumer.poll(Duration.ofMillis(100));
            logger.info("queryAlarms: subpartitionName='{}' - poll: count={}", subpartitionName, records.count());

            for (ConsumerRecord<String, byte[]> record: records) {
                notificationIdentifiers.add(record.key());
                values.add( new String(record.value(), StandardCharsets.UTF_8) );

                if (notificationIdentifiers.size() >= howMany) {
                    // We do not want to return more results.
                    break;
                }
            }

        } while (! records.isEmpty());

        // Prepare the result.
        result.alarmNotificationIdentifiers = notificationIdentifiers.toArray(new String[notificationIdentifiers.size()]);
        result.alarmValues = values.toArray(new String[values.size()]);

        // Read the next possible position. (It does not necessarily  mean that at the moment there is an alarm available.)
        //
        long currentPosition = consumer.position(partition);
        if (currentPosition >= subpartitionEndOffset) {
            // There are no more alarms.
            result.tagOfTheNextAvailableAlarm = null;
        } else {
            // There are more alarms available.  (i.e. they can be retrieved by another call)
            result.tagOfTheNextAvailableAlarm = String.valueOf(currentPosition);
        }

        logger.info("queryAlarms: subpartitionName='{}' - success", subpartitionName);

        return result;
    }

    @Override
    public String[] getSubpartitions(RawAlarmsPartitionDefinition partitionDefinition) {
        return new String[] {"0"};  //       @TODO
    }
}
