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
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaasDaoKafkaTest {

    private static final String ZKHOST = "127.0.0.1";
    private static final String BROKERHOST = "127.0.0.1";
    private static final String BROKERPORT = "9092";

    private static EmbeddedZookeeper zkServer;
    private static KafkaZkClient zkClient;
    private static KafkaServer kafkaServer;

    private static RaasDaoKafkaTestConfiguration testConfig;
    protected RaasDao kafkaDao;
    private RaasDaoTestScenarios scenarios;

    @BeforeClass
    public static void init() throws IOException {

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
        brokerProps.setProperty("listeners", "PLAINTEXT://" + BROKERHOST +":" + BROKERPORT);
        brokerProps.setProperty("offsets.topic.replication.factor" , "1");
        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);

        // Connect to the embedded Kafka.
        testConfig = new RaasDaoKafkaTestConfiguration();

        // Create topic.
        //
        Boolean isSecure = false;
        int sessionTimeoutMs = 200000;
        int connectionTimeoutMs = 15000;
        int maxInFlightRequests = 10;
        Time time = Time.SYSTEM;
        String metricGroup = "myGroup";
        String metricType = "myType";
        zkClient = KafkaZkClient.apply(zkConnect, isSecure, sessionTimeoutMs,
                connectionTimeoutMs, maxInFlightRequests, time, metricGroup, metricType);

        AdminZkClient adminZkClient = new AdminZkClient(zkClient);

        int partitions = 1;
        int replication = 1;
        Properties topicConfig = new Properties();

        adminZkClient.createTopic(testConfig.getTopicName(), partitions, replication,
                topicConfig, RackAwareMode.Disabled$.MODULE$);   // @TODO topic compaction
    }

    @AfterClass
    public static void cleanup() {
        testConfig.close();

        kafkaServer.shutdown();
        zkClient.close();

        // zkServer.shutdown();  @TODO uncomment when a patch is available for https://issues.apache.org/jira/browse/KAFKA-6291#
    }

    @Before
    public void initDao() {
        // Create bean to be tested.
        this.kafkaDao = testConfig.getDao();
        scenarios = new RaasDaoTestScenarios(this.kafkaDao);
    }

    @Test
    public void t1_whenCreatedANewAlarm_thenReturnIt() {
        scenarios.t1_whenCreatedANewAlarm_thenReturnIt();
    }

    @Test
    public void t2_whenAlarmExists_thenDataShouldSurviveDaoReconnect() {
        // Disconnect.
        testConfig.close();
        // Connect again to the embedded DB.  (do not create test data again !)
        testConfig = new RaasDaoKafkaTestConfiguration();
        kafkaDao = testConfig.getDao();
        scenarios = new RaasDaoTestScenarios(this.kafkaDao);

        scenarios.t2_whenAlarmExists_thenShouldBeReturned();
    }

    @Test
    public void t3_whenUpsertingAnExistingAlarm_thenUpdateIt() {
        scenarios.t3_whenUpsertingAnExistingAlarm_thenUpdateIt();
    }

    @Test
    public void t4_whenRemovedAnExistingAlarm_thenCreateIt() {
        scenarios.t4_whenRemovedAnExistingAlarm_thenCreateIt();
    }

    @Test
    public void t5_whenPutThreeAlarms_thenKeepTheirOrder() {
        scenarios.t5_whenPutThreeAlarms_thenKeepTheirOrder();
    }

}
