package com.j9soft.poc.alarms;

import kafka.admin.RackAwareMode;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.zk.AdminZkClient;
import kafka.zk.EmbeddedZookeeper;
import kafka.zk.KafkaZkClient;
import org.apache.kafka.common.utils.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;


public class RaasDaoKafkaTestEmbeddedBroker {

    private static final String ZKHOST = "127.0.0.1";
    private static final String BROKERHOST = "127.0.0.1";
    private static final String BROKERPORT = "9092";

    private static EmbeddedZookeeper zkServer;
    private static KafkaZkClient zkClient;
    private static KafkaServer kafkaServer;

    public void init() throws IOException {

        // Start an embedded Kafka Server
        //
        // setup Zookeeper
        zkServer = new EmbeddedZookeeper();
        String zkConnect = ZKHOST + ":" + zkServer.port();
        //
        // setup Broker
        Properties brokerProps = new Properties();
        brokerProps.setProperty("zookeeper.connect", zkConnect);
        brokerProps.setProperty("broker.id", "0");
        brokerProps.setProperty("log.dirs", Files.createTempDirectory("kafka-").toAbsolutePath().toString());
        brokerProps.setProperty("listeners", "PLAINTEXT://" + BROKERHOST + ":" + BROKERPORT);
        brokerProps.setProperty("offsets.topic.replication.factor", "1");
        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);
    }

    public void createTopic(String topicName) {

        // Create topic.
        //
        Boolean isSecure = false;
        int sessionTimeoutMs = 200000;
        int connectionTimeoutMs = 15000;
        int maxInFlightRequests = 10;
        Time time = Time.SYSTEM;
        String metricGroup = "myGroup";
        String metricType = "myType";
        String zkConnect = ZKHOST + ":" + zkServer.port();
        zkClient = KafkaZkClient.apply(zkConnect, isSecure, sessionTimeoutMs,
                connectionTimeoutMs, maxInFlightRequests, time, metricGroup, metricType);

        AdminZkClient adminZkClient = new AdminZkClient(zkClient);

        int partitions = 1;
        int replication = 1;
        Properties topicConfig = new Properties();

        adminZkClient.createTopic(topicName, partitions, replication,
                topicConfig, RackAwareMode.Disabled$.MODULE$);   // @TODO topic compaction
    }

    public void close() {
        kafkaServer.shutdown();
        zkClient.close();

        // zkServer.shutdown();  @TODO uncomment when a patch is available for https://issues.apache.org/jira/browse/KAFKA-6291#
    }

}
