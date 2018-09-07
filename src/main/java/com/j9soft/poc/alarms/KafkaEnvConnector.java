package com.j9soft.poc.alarms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Configuration settings are read from system properties named "kafka-host" and "kafka-port".
 */
@Profile({"prod","kafka-dev"})
@Component
public class KafkaEnvConnector extends KafkaConnector {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEnvConnector.class);

    @Autowired
    KafkaEnvConnector(@Value("${kafka-host}") String brokerHost, @Value("${kafka-port:9092}") Integer brokerPort) {
        if (logger.isInfoEnabled()) {
            logger.info("Connecting to Kafka with system properties: {}", Arrays.asList(
                    System.getProperties().entrySet().stream()
                            .filter(e -> ((String) e.getKey()).startsWith("kafka"))
                            .toArray())
            );
            logger.info("Connecting to Kafka at: {}:{}", brokerHost, brokerPort);
        }

        connect(brokerHost, brokerPort);
    }
}
