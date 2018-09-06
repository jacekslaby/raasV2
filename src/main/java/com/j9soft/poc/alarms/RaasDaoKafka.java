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
import org.springframework.context.annotation.Profile;
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
@Profile("Production")
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

        // Prepare from which Kafka partition we will read.
        //
        TopicPartition partition = new TopicPartition(topicName, Integer.parseInt(subpartitionName));
        Collection<TopicPartition> partitions = Arrays.asList(partition);

        // Calculate current end of data in this Kafka partition.
        // (We need this to calculate whether there is a next pack available.)
        //
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
        long subpartitionEndOffset =  endOffsets.get(partition);

        // Rewind to the beginning of data. (either start from zero (in case of the first pack) or a designated place)
        //
        consumer.assign(partitions);  // btw: We do not use subscribe(), i.e. we do not use kafka's built-in group coordination.
        if (tagOfTheFirstAlarmToBeReturned == null) {
            // We want to load all contents. (i.e. its the first pack)
            logger.info("queryAlarms: subpartitionName='{}' - seekToBeginning", subpartitionName);
            consumer.seekToBeginning(partitions);
        } else {
            logger.info("queryAlarms: subpartitionName='{}' - seek:", tagOfTheFirstAlarmToBeReturned);
            consumer.seek(partition, Long.parseLong(tagOfTheFirstAlarmToBeReturned)); // We want to load another pack.
        }

        // Read the data.
        //
        Map<String, Integer> loadedAlarms = new HashMap<>();
        List<String> notificationIdentifiers = new ArrayList<>(howMany);
        List<String> values = new ArrayList<>(howMany);
        ConsumerRecords<String, byte[]> records;
        String key;
        byte[] value;
        int resultCounter = 0;
        Integer lastIndex;
        do {
            // Load from Kafka.
            records = consumer.poll(Duration.ofMillis(100));
            logger.info("queryAlarms: subpartitionName='{}' - poll: count={}", subpartitionName, records.count());

            for (ConsumerRecord<String, byte[]> record: records) {
                logger.debug("queryAlarms: subpartitionName='{}' - poll: record={}", subpartitionName, record);
                resultCounter++;
                key = record.key();
                value = record.value();

                // Remove duplicated keys.
                // (I.e. it is like a "compaction" within a pack. We overwrite old alarm value with a new one.)
                //
                lastIndex = loadedAlarms.get(key);
                if (lastIndex != null) {
                    // We will ignore the value previously gathered for the same Notification Identifier.
                    resultCounter--;
                    notificationIdentifiers.set(lastIndex, null); // we will not use this old value so set null
                }

                // Remove keys with null value. (Note: It is possible to do this in the first pack only.)
                //
                if (tagOfTheFirstAlarmToBeReturned == null   // are we loading the first pack ?
                        && value == null) {

                    resultCounter--;
                    continue; // We simply ignore this alarm.  (AND we support a case that it was removed within the first pack !)
                }

                // We gather this alarm for the result.
                //
                loadedAlarms.put(key, notificationIdentifiers.size());
                notificationIdentifiers.add(key);
                if (value != null) {
                    values.add( new String(record.value(), StandardCharsets.UTF_8) );
                } else {
                    values.add(null);  // In the second and the next packs we must deliver nulls because they may concern alarms from the previous packs.
                }

                if (resultCounter >= howMany) {
                    // Client does not want more results.
                    break;
                }
            }

        } while ( !records.isEmpty() );

        // Prepare the result.
        //
        result.alarmNotificationIdentifiers = new String[resultCounter];
        result.alarmValues = new String[resultCounter];
        int resultIndex = 0;
        for (int i = 0; i < notificationIdentifiers.size(); i++) {
            if (notificationIdentifiers.get(i) == null) {
                continue;  // this alarm is ignored because it got overwritten
            }
            result.alarmNotificationIdentifiers[resultIndex] = notificationIdentifiers.get(i);
            result.alarmValues[resultIndex] = values.get(i);
            resultIndex++;
        }

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
    public void removeAlarm(String domain, String adapterName, String notificationIdentifier) {
        ProducerRecord<String, byte[]> data = new ProducerRecord<>(
                topicName, notificationIdentifier, null);  // null is a marker for a deleted record

        logger.info("removeAlarm: '{}' - start", notificationIdentifier);
        try {
            // @TODO save to a subpartition assigned to this adapter

            producer.send(data).get(); // We want to save it immediately.

        } catch (InterruptedException|ExecutionException e) {
            logger.info("removeAlarm: '{}' - failure", notificationIdentifier);
            throw new RuntimeException("Failed to remove the alarm: " + notificationIdentifier, e);  // @TODO add exception to API
        }

        logger.info("removeAlarm: '{}' - success", notificationIdentifier);
    }

    @Override
    public String[] getSubpartitions(RawAlarmsPartitionDefinition partitionDefinition) {
        return new String[] {"0"};  //       @TODO
    }
}
